package com.trademaster;

import com.google.inject.Provides;
import com.trademaster.controllers.HomeController;
import com.trademaster.db.models.PlayerData;
import com.trademaster.db.models.WealthData;
import com.trademaster.models.HomeModel;
import com.trademaster.services.AutoSaveService;
import com.trademaster.services.DbService;
import com.trademaster.services.WealthDataService;
import com.trademaster.views.home.HomeView;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
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
    @Inject
    private WealthDataService wealthDataService;


    private NavigationButton navButton;
    private HomeModel model;
    private HomeController controller;
    private boolean playerInitialized = false;


    @Override
    protected void startUp() throws Exception {
        log.info("Trade Master started!");

        playerInitialized = false;

        model = new HomeModel();
        controller = new HomeController(config, model);
        HomeView view = new HomeView(controller);

        wealthDataService.attachModel(model);
        wealthDataService.attachController(controller);

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
            autoSaveService.start(dbService.get());
        }

        PlayerData playerData = dbService.get().getDbFileData();
        if (playerData != null) {
            model.loadWealthDataFromFile(playerData);
            controller.refresh();
        }
    }

}
