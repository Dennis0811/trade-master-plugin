package com.trademaster;

import com.google.inject.Provides;
import com.trademaster.controllers.HomeController;
import com.trademaster.db.models.PlayerData;
import com.trademaster.db.models.WealthData;
import com.trademaster.models.HomeModel;
import com.trademaster.services.AutoSaveService;
import com.trademaster.services.DbService;
import com.trademaster.views.home.HomeView;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetID;
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
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;

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


    private NavigationButton navButton;
    private HomeModel model;
    private HomeController controller;
    private boolean playerInitialized = false;
    private PlayerData playerData;

    private final WealthData WEALTH_DATA = new WealthData();


    @Override
    protected void startUp() throws Exception {
        log.info("Trade Master started!");

        playerInitialized = false;

        model = new HomeModel();
        controller = new HomeController(config, model);
        HomeView view = new HomeView(controller);

        PlayerData preliminaryDbData = dbService.getFallBackData();
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
        log.info("Trade Master stopped!");

        autoSaveService.stop();
        dbService.close();

        playerInitialized = false;
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown clientShutdown) {
        log.info("Client shuts down!");

        autoSaveService.stop();
        dbService.close();
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

    private void initDbSession() {
        Player player = client.getLocalPlayer();

        if (player == null || player.getName() == null) {
            clientThread.invokeLater(this::initDbSession);
            return;
        }

        playerInitialized = true;

        dbService.create(player.getName(), WEALTH_DATA);

        if (config.autoSaveEnabled()) {
            autoSaveService.start(dbService.get());
        }

        PlayerData playerData = dbService.get().getDbFileData();
        if (playerData != null) {
            model.loadWealthDataFromFile(playerData);
            controller.refresh();
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
        if (widgetLoaded.getGroupId() == WidgetID.BANK_GROUP_ID) {
            ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);

            if (bankContainer != null) {
                Item[] items = bankContainer.getItems();
                long bankWealth = 0;

                try {
                    for (Item item : items) {
                        int itemId = item.getId();
                        int itemQuantity = item.getQuantity();
                        bankWealth += (long) itemManager.getItemPrice(itemId) * itemQuantity; //TODO: what fucking price is this using ???
                    }
                    model.setBankWealth(bankWealth);
                    WEALTH_DATA.setBankWealth(bankWealth);
                    controller.refresh();
                } catch (Exception e) {
                    log.warn("Failed to fetch GE price for bank: {}", bankWealth);
                }
            }
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged grandExchangeOfferChanged) {
        GrandExchangeOffer[] geOffers = client.getGrandExchangeOffers();
        long geWealth = 0;

        try {
            for (GrandExchangeOffer offer : geOffers) {
                int itemQuantity = offer.getTotalQuantity();
                int itemPrice = offer.getPrice();
                geWealth += (long) itemPrice * itemQuantity; //TODO: what fucking price is this using ???
            }

            model.setGeWealth(geWealth);
            WEALTH_DATA.setGeWealth(geWealth);
            controller.refresh();
        } catch (Exception e) {
            log.warn("Failed to fetch GE price for GE: {}", geWealth);
        }
    }


    @Subscribe
    public void onGameTick(GameTick event) {
        ItemContainer invContainer = client.getItemContainer(InventoryID.INVENTORY);

        if (invContainer != null) {
            Item[] items = invContainer.getItems();
            long invWealth = 0;

            try {
                for (Item item : items) {
                    int itemId = item.getId();
                    int itemQuantity = item.getQuantity();
                    invWealth += (long) itemManager.getItemPrice(itemId) * itemQuantity; //TODO: what fucking price is this using ???
                }
                model.setInventoryWealth(invWealth);
                WEALTH_DATA.setInventoryWealth(invWealth);
                controller.refresh();
            } catch (Exception e) {
                log.warn("Failed to fetch GE price for inventory: {}", invWealth);
            }
        }
    }


    @Provides
    TradeMasterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TradeMasterConfig.class);
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
                    autoSaveService.start(dbService.get());
                } else {
                    autoSaveService.stop();
                }
            }
        }
    }
}
