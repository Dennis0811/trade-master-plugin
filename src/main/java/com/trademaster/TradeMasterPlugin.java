package com.trademaster;

import com.google.inject.Provides;
import com.trademaster.controllers.HomeController;
import com.trademaster.db.models.PlayerWealthData;
import com.trademaster.models.HomeModel;
import com.trademaster.services.AutoSaveService;
import com.trademaster.services.DbService;
import com.trademaster.services.GEPriceService;
import com.trademaster.services.WealthDataService;
import com.trademaster.services.models.GEItemPriceData;
import com.trademaster.utils.NumberFormatUtils;
import com.trademaster.views.home.HomeView;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.gameval.ItemID;


import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Slf4j
@PluginDescriptor(
        name = "Trade Master",
        description = "Make GP the easy way! Provides advanced Grand Exchange tracking and item management."
)
public class TradeMasterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private TradeMasterConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private AutoSaveService autoSaveService;
    @Inject
    private DbService dbService;
    @Inject
    private WealthDataService wealthDataService;
    @Inject
    GEPriceService gePriceService;
    @Inject
    TooltipManager tooltipManager;

    private NavigationButton navButton;
    private HomeModel model;
    private HomeController controller;
    private boolean playerInitialized = false;
    private int lastHoveredItemId;
    private GEItemPriceData priceData;
    private int fallbackPrice;
    private int itemQuantity;
    private boolean showLastBuyPrice;
    private boolean showLastSellPrice;
    private boolean showLastBuyTime;
    private boolean showLastSellTime;
    private boolean showHaPrice;
    private boolean geTooltipEnabled;


    @Override
    protected void startUp() throws Exception {
        log.info("Trade Master started!");

        playerInitialized = false;

        model = new HomeModel();
        controller = new HomeController(config, model);
        HomeView view = new HomeView(controller);

        wealthDataService.attachModel(model);

        PlayerWealthData preliminaryDbData = dbService.getFallBackData();
        if (preliminaryDbData != null) {
            model.loadWealthDataFromFile(preliminaryDbData);
            controller.refresh();
        }

        showLastBuyPrice = config.showLastBuyPrice();
        showLastSellPrice = config.showLastSellPrice();
        showLastBuyTime = config.showLastBuyTime();
        showLastSellTime = config.showLastSellTime();
        showHaPrice = config.showHaPrice();
        geTooltipEnabled = config.geTooltipEnabled();

        navButton = NavigationButton.builder()
                .tooltip("Trade Master")
                .icon(ImageUtil.loadImageResource(TradeMasterPlugin.class, "/icon.png"))
                .priority(2)
                .panel(view)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        autoSaveService.stop();
        dbService.close();

        playerInitialized = false;
        clientToolbar.removeNavigation(navButton);
        log.info("Trade Master stopped!");
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown clientShutdown) {
        autoSaveService.stop();
        dbService.close();

        log.info("Client shuts down!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN
                && !playerInitialized) {
            clientThread.invokeLater(this::initDbSession);
        } else {
            playerInitialized = false;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
        if (itemContainerChanged.getContainerId() == InventoryID.BANK.getId()) {
            wealthDataService.updateBank(itemContainerChanged.getItemContainer());
        }

        if (itemContainerChanged.getContainerId() == InventoryID.INVENTORY.getId()) {
            wealthDataService.updateInventory(itemContainerChanged.getItemContainer());
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged grandExchangeOfferChanged) {
        GrandExchangeOffer[] geOffers = client.getGrandExchangeOffers();
        wealthDataService.updateGe(geOffers);
    }

    @Subscribe
    public void onBeforeRender(BeforeRender beforeRender) {
        if (client.isMenuOpen() || !geTooltipEnabled) return;
        MenuEntry menuEntry = getLastMenuEntry();
        if (menuEntry == null || !shouldEnableTooltip(menuEntry)) return;

        if (priceData == null) {
            tooltipManager.add(new Tooltip("Loading..."));
            return;
        }

        int itemId = menuEntry.getItemId();
        if (itemId < 1) return;
        ItemComposition comp = itemManager.getItemComposition(itemId);

        if (comp.isTradeable()) {
            tooltipManager.add(new Tooltip(formatTooltip(priceData, comp)));
        }
        String formattedTooltipString = formatTooltip(priceData, fallbackPrice, comp);
        if (formattedTooltipString != null) {
            tooltipManager.add(new Tooltip(formattedTooltipString));
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (!geTooltipEnabled) return;
        MenuEntry menuEntry = menuEntryAdded.getMenuEntry();
        if (menuEntry == null) return;
        int itemId = menuEntry.getItemId();
        if (lastHoveredItemId == itemId || itemId < 1 || !shouldEnableTooltip(menuEntry)) return;

        lastHoveredItemId = itemId;

        if (itemManager.getItemComposition(itemId).isTradeable()) {
            CompletableFuture.runAsync(() -> {
                priceData = gePriceService.getPrice(itemId);
            });
        } else {
            fallbackPrice = gePriceService.getFallbackPrice(itemId);
        }

        Widget widget = menuEntry.getWidget();
        if (widget == null) return;
        itemQuantity = widget.getItemQuantity();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals("trademaster")) {
            switch (configChanged.getKey()) {
                case "autoSaveInterval":
                    autoSaveService.reschedule();
                    break;
                case "autoSaveEnabled":
                    if (config.autoSaveEnabled()) {
                        autoSaveService.start(dbService);
                    } else {
                        autoSaveService.stop();
                    }
                    break;
                case "geTooltipEnabled":
                    geTooltipEnabled = config.geTooltipEnabled();
                    break;
                case "showLastBuyPrice":
                    showLastBuyPrice = config.showLastBuyPrice();
                    break;
                case "showLastSellPrice":
                    showLastSellPrice = config.showLastSellPrice();
                    break;
                case "showLastBuyTime":
                    showLastBuyTime = config.showLastBuyTime();
                    break;
                case "showLastSellTime":
                    showLastSellTime = config.showLastSellTime();
                    break;
                case "showHaPrice":
                    showHaPrice = config.showHaPrice();
                    break;
                case "abbreviateThreshold":
                case "abbreviateGpTotalEnabled":
                case "abbreviateHoverGpTotalEnabled":
                case "abbreviateHoverBankEnabled":
                case "abbreviateHoverInventoryEnabled":
                case "abbreviateHoverGeEnabled":
                    controller.refresh();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (!wealthDataService.isRefreshPending()) return;

        controller.refresh();
        wealthDataService.clearRefreshPending();
    }


    @Provides
    TradeMasterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TradeMasterConfig.class);
    }


    private void initDbSession() {
        Player player = client.getLocalPlayer();

        if (player == null || player.getName() == null) {
            clientThread.invokeLater(this::initDbSession);
            return;
        }

        playerInitialized = true;

        dbService.create(player.getName(), wealthDataService.getWealthData());

        if (config.autoSaveEnabled()) {
            autoSaveService.start(dbService);
        }

        PlayerWealthData playerWealthData = dbService.get().getDbFileData();
        if (playerWealthData != null) {
            model.loadWealthDataFromFile(playerWealthData);
        }
    }

    private String createCustomMenuEntry(int lastBuyPrice, int lastSellPrice, long lastBuyTime, long lastSellTime, int haPrice) {
        StringBuilder formatString = new StringBuilder();
        ArrayList<Object> arrayList = new ArrayList<>();

        String lastBuyPriceString = NumberFormatUtils.formatNumber(lastBuyPrice);
        String lastSellPriceString = NumberFormatUtils.formatNumber(lastSellPrice);
        String haPriceString = NumberFormatUtils.formatNumber(haPrice);

        if (showLastBuyPrice) {
            formatString.append("%s: %s GP</br>");
            arrayList.add("Last GE Buy Price");
            arrayList.add(lastBuyPriceString);
        }
        if (showLastSellPrice) {
            formatString.append("%s: %s GP</br>");
            arrayList.add("Last GE Sell Price");
            arrayList.add(lastSellPriceString);
        }
        if (showLastBuyTime) {
            formatString.append("%s: %s</br>");
            arrayList.add("Last GE Buy Time");
            arrayList.add(timeAgo(lastBuyTime));
        }
        if (showLastSellTime) {
            formatString.append("%s: %s</br>");
            arrayList.add("Last GE Sell Time");
            arrayList.add(timeAgo(lastSellTime));
        }
        if (showHaPrice && haPrice > 0) {
            formatString.append("%s: %s GP</br>");
            arrayList.add("HA");
            arrayList.add(haPriceString);
        }
        return String.format(formatString.toString(), arrayList.toArray());
    }

    private String convertUnixTimeToLocal(long unixTime) {
        Instant instant = Instant.ofEpochSecond(unixTime);
        ZonedDateTime localTime = instant.atZone(ZoneId.systemDefault());
        return localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy"));
    }

    private String timeAgo(long unixTime) {
        long now = System.currentTimeMillis();
        long diff = now - unixTime * 1000;

        if (diff < 0) {
            diff = 0;
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return seconds + " sec ago";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24) return hours + " hours ago";
        return days + " days ago";
    }

    private boolean shouldEnableTooltip(MenuEntry menuEntry) {
        int itemId = menuEntry.getItemId();
        if (itemId < 1) return false;
        Widget widget = menuEntry.getWidget();
        return widget != null &&
                (WidgetInfo.INVENTORY.getId() == widget.getId()
                        || WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId() == widget.getId()
                        || WidgetInfo.BANK_ITEM_CONTAINER.getId() == widget.getId()
                        || WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER.getId() == widget.getId()
                );
    }

    private MenuEntry getLastMenuEntry() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        int lastEntryId = menuEntries.length - 1;
        if (lastEntryId < 0) return null;
        return menuEntries[lastEntryId];
    }

    private String formatTooltip(GEItemPriceData data, ItemComposition comp) {
        return ColorUtil.prependColorTag(
                createCustomMenuEntry(
                        data.getHigh(), data.getLow(), data.getHighTime(), data.getLowTime(), comp.getHaPrice()
                ),
                Color.WHITE
        );
    }

    private String formatTooltip(GEItemPriceData priceData, int fallbackPrice, ItemComposition comp) {
        int usedPrice = fallbackPrice;
        boolean itemIsTradeable = comp.isTradeable();

        if (priceData != null && itemIsTradeable) {
            usedPrice = priceData.getLow();
        }

        StringBuilder formatString = new StringBuilder();
        ArrayList<String> arrayList = new ArrayList<>();
        long priceQuantity = (long) usedPrice * itemQuantity;
        long haPriceQuantity = (long) comp.getHaPrice() * itemQuantity;

        if (comp.getId() == ItemID.COINS || comp.getId() == ItemID.PLATINUM) {
            formatString.append("%s GP</br>");
            arrayList.add(NumberFormatUtils.formatNumber(priceQuantity));
        } else if (priceQuantity > 0 && itemIsTradeable) {
            formatString.append("%s GP");
            arrayList.add(NumberFormatUtils.formatNumber(priceQuantity));

            if (itemQuantity > 1) {
                formatString.append(" (%s ea)");
                arrayList.add(NumberFormatUtils.formatNumber(usedPrice));
            }
            formatString.append("</br>");
        }

        if (showHaPrice && haPriceQuantity > 0) {
            formatString.append("%s: %s GP");
            arrayList.add("HA");
            arrayList.add(String.valueOf(haPriceQuantity));
        }

        if (formatString.toString().isEmpty()) return null;

        return ColorUtil.prependColorTag(String.format(formatString.toString(), arrayList.toArray()), Color.WHITE);
    }
}
