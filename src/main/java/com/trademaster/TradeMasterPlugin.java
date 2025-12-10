package com.trademaster;

import com.google.inject.Provides;
import com.sun.jna.platform.win32.WinDef;
import com.trademaster.controllers.HomeController;
import com.trademaster.models.HomeModel;
import com.trademaster.views.home.HomeView;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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
    private ClientToolbar clientToolbar;

    @Inject
    private TradeMasterConfig config;

    @Inject
    private ItemManager itemManager;

    private NavigationButton navButton;

    final long BANK_NET_WORTH = (long) (Math.random() * Math.pow(10, 12));
    final long INVENTORY_NET_WORTH = (long) (Math.random() * Math.pow(10, 12));


    @Override
    protected void startUp() throws Exception {
        log.debug("Example started!");

        HomeModel model = new HomeModel(BANK_NET_WORTH, INVENTORY_NET_WORTH);
        HomeController controller = new HomeController(model);
        HomeView panel = new HomeView(controller);

        navButton = NavigationButton.builder()
                .tooltip("Trade Master")
                .icon(ImageUtil.loadImageResource(TradeMasterPlugin.class, "/icon.png"))
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        log.debug("Example stopped!");
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        Item[] items = bank.getItems();

        for (Item item : items) {
            int itemId = item.getId();
            ItemComposition comp = itemManager.getItemComposition(itemId);
            long itemPrice = itemManager.getItemPrice(itemId); // TODO: replace this with actual logic to get GE Price
        }
    }


    @Provides
    TradeMasterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TradeMasterConfig.class);
    }
}
