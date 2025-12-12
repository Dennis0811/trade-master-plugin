package com.trademaster;

import com.google.inject.Provides;
import com.trademaster.controllers.HomeController;
import com.trademaster.db.DbManager;
import com.trademaster.db.models.PlayerData;
import com.trademaster.db.models.WealthData;
import com.trademaster.models.HomeModel;
import com.trademaster.views.home.HomeView;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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

    private NavigationButton navButton;
    private HomeModel model;
    private HomeController controller;
    private boolean playerInitialized = false;
    private DbManager db;
    private PlayerData playerData;

    private final WealthData WEALTH_DATA = new WealthData();


    @Override
    protected void startUp() throws Exception {
        log.info("Trade Master started!");

        playerInitialized = false;

        if (client.getGameState() == GameState.LOGGED_IN) {
            clientThread.invokeLater(this::createDbManager);
        }

        DbManager earlyDb = new DbManager();

        if (earlyDb.dbFileExists()) {
            playerData = earlyDb.getDbFileData();
        }

        model = new HomeModel();

        if (playerData != null) {
            model.loadWealthDataFromFile(playerData);
        }

        controller = new HomeController(config, model);
        HomeView view = new HomeView(controller);

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

        saveDbData();

        playerInitialized = false;
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown clientShutdown) {
        log.info("Client shuts down!");

        saveDbData();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN
                && !playerInitialized) {
            clientThread.invokeLater(this::createDbManager);
        } else {
            playerInitialized = false;
        }
    }

    private void createDbManager() {
        final Player player = client.getLocalPlayer();

        if (player == null) {
            clientThread.invokeLater(this::createDbManager);
            return;
        }

        final String name = player.getName();

        if (name == null || name.isEmpty()) {
            clientThread.invokeLater(this::createDbManager);
            return;
        }

        // Success
        playerInitialized = true;
        db = new DbManager(name, WEALTH_DATA);

        // Fill model with actual DB data, now that we can get that
        playerData = db.getDbFileData();

        if (playerData != null) {
            model.loadWealthDataFromFile(playerData);
            controller.refresh();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // TODO: I dont want to do this onGameTick but every 60s
        ItemContainer invContainer = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
        GrandExchangeOffer[] geOffers = client.getGrandExchangeOffers();

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


    @Provides
    TradeMasterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TradeMasterConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals("trademaster")) {
            controller.refresh();
        }
    }


    private void saveDbData() {
        if (db != null) {
            db.writeToFile();
        } else {
            log.warn("db is null!");
        }
    }
}
