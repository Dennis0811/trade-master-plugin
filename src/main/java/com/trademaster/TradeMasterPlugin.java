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

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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


    @Override
    protected void startUp() throws Exception {
        log.info("Trade Master started!");

        playerInitialized = false;

        model = new HomeModel();
        controller = new HomeController(config, model);
        HomeView view = new HomeView(controller);

        wealthDataService.attachModel(model);
        wealthDataService.attachController(controller);

        PlayerWealthData preliminaryDbData = dbService.getFallBackData();
        if (preliminaryDbData != null) {
            model.loadWealthDataFromFile(preliminaryDbData);
            controller.refresh();
        }

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
        if (client.isMenuOpen()) {
            return;
        }

        MenuEntry menuEntry = getLastMenuEntry();

        if (menuEntry == null) return;

        int itemId = menuEntry.getItemId();

        if (itemId < 1) return;

        ItemComposition itemComp = itemManager.getItemComposition(itemId);

        if (!itemComp.isTradeable()) return;

        Widget widget = menuEntry.getWidget();

        if (shouldEnableTooltip(widget)) {
            int lastBuyPrice = priceData.getHigh();
            int lastSellPrice = priceData.getLow();
            long lastBuyTime = priceData.getHighTime();
            long lastSellTime = priceData.getLowTime();
            int highAlchemyPrice = itemComp.getHaPrice();
            String customMenuEntryText = createCustomMenuEntry(lastBuyPrice, lastSellPrice, lastBuyTime, lastSellTime, highAlchemyPrice);
            String formattedText = ColorUtil.prependColorTag(customMenuEntryText, Color.WHITE);
            tooltipManager.add(new Tooltip(formattedText));
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        MenuEntry menuEntry = getLastMenuEntry();
        int itemId = menuEntry.getItemId();

        if (lastHoveredItemId == itemId || itemId < 1) {
            return;
        }

        ItemComposition itemComp = itemManager.getItemComposition(itemId);

        if (!itemComp.isTradeable()) {
            return;
        }

        Widget widget = menuEntry.getWidget();

        if (shouldEnableTooltip(widget)) {
            priceData = gePriceService.getPrice(itemId);
            lastHoveredItemId = itemId;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals("trademaster")) {
            controller.refresh();

            if (configChanged.getKey().equals("autoSaveInterval")) {
                autoSaveService.reschedule();
            }
            if (configChanged.getKey().equals("autoSaveEnabled")) {
                if (config.autoSaveEnabled()) {
                    autoSaveService.start(dbService);
                } else {
                    autoSaveService.stop();
                }
            }
        }
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
            controller.refresh();
        }
    }

    private String createCustomMenuEntry(int lastBuyPrice, int lastSellPrice, long lastBuyTime, long lastSellTime, int highAlchemyPrice) {
        return String.format(
                "%s: %d GP<br>%s: %d GP<br>%s: %s<br>%s: %s<br>%s: %d GP",
                "Last GE Buy Price", lastBuyPrice,
                "Last GE Sell Price", lastSellPrice,
                "Last GE Buy Time", timeAgo(lastBuyTime),
                "Last GE Sell Time", timeAgo(lastSellTime),
                "HA", highAlchemyPrice
        );
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

        if (seconds < 60) return seconds + "sec ago";
        if (minutes < 60) return minutes + "min ago";
        if (hours < 24) return hours + "h ago";
        return days + " days ago";
    }

    private boolean shouldEnableTooltip(Widget widget) {
        return widget != null && (WidgetInfo.INVENTORY.getId() == widget.getId()
                || WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId() == widget.getId()
                || WidgetInfo.BANK_ITEM_CONTAINER.getId() == widget.getId());
    }

    private MenuEntry getLastMenuEntry() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        int lastEntryId = menuEntries.length - 1;
        if (lastEntryId < 0) return null;
        return menuEntries[lastEntryId];
    }
}
