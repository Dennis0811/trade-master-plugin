
package com.trademaster.services;

import com.trademaster.TradeMasterConfig;
import com.trademaster.services.models.GEItemPriceData;
import com.trademaster.utils.NumberFormatUtils;
import com.trademaster.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class CustomTooltipService {
    @Inject
    private Client client;
    @Inject
    private TooltipManager tooltipManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private TradeMasterConfig config;
    @Inject
    private GEPriceService gePriceService;
    @Inject
    private ClientThread clientThread;

    private GEItemPriceData priceData;
    private int fallbackPrice;
    private int lastHoveredItemId;
    private int itemQuantity;
    private MenuEntry lastMenuEntry;


    public void handleOnBeforeRender() {
        if (client.isMenuOpen() || !config.geTooltipEnabled()) return;
        MenuEntry menuEntry = lastMenuEntry;

        if (menuEntry == null || !shouldEnableTooltip(menuEntry)) {
            lastHoveredItemId = -1;
            priceData = null;
            fallbackPrice = 0;
            return;
        }

        int itemId = getCanonIdWithGroundCheck(menuEntry);

        if (itemId < 1) return;

        ItemComposition comp = itemManager.getItemComposition(itemId);

        if (comp.isTradeable() && priceData != null) {
            String tooltipText = buildGESection(priceData);

            if (tooltipText != null) {
                tooltipManager.add(new Tooltip(tooltipText));
            }
        }
        if (config.showSummarySection()) {
            String formattedTooltipString = buildSummarySection(priceData, fallbackPrice, comp);
            if (formattedTooltipString != null) {
                tooltipManager.add(new Tooltip(formattedTooltipString));
            }
        }
    }

    public void handleOnMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (!config.geTooltipEnabled()) return;

        MenuEntry menuEntry = menuEntryAdded.getMenuEntry();
        if (menuEntry == null) return;
        lastMenuEntry = menuEntry;

        int itemId = getCanonIdWithGroundCheck(menuEntry);


        if (lastHoveredItemId == itemId
                || itemId < 1
                || !shouldEnableTooltip(menuEntry))
            return;

        priceData = null;
        fallbackPrice = 0;
        ItemComposition comp = itemManager.getItemComposition(itemId);

        if (isGroundItem(menuEntry)) {
            itemQuantity = getGroundItemQuantity(menuEntry);
        } else {
            Widget widget = menuEntry.getWidget();
            if (widget == null) return;
            itemQuantity = widget.getItemQuantity();
        }

        if (comp.isTradeable()) {
            CompletableFuture
                    .supplyAsync(() -> gePriceService.getPrice(itemId))
                    .thenAccept(data ->
                            clientThread.invokeLater(() -> {
                                if (lastHoveredItemId == itemId) {
                                    priceData = data;
                                }
                            })
                    );
        } else {
            fallbackPrice = gePriceService.getFallbackPrice(itemId);
        }
        lastHoveredItemId = itemId;
    }


    private String buildGESection(GEItemPriceData data) {
        StringBuilder formatString = new StringBuilder();

        int lastBuyPrice = data.getHigh();
        int lastSellPrice = data.getLow();
        long lastBuyTime = data.getHighTime();
        long lastSellTime = data.getLowTime();

        String lastBuyPriceString = NumberFormatUtils.abbreviateNumber(lastBuyPrice);
        String lastSellPriceString = NumberFormatUtils.abbreviateNumber(lastSellPrice);

        if (config.showLastBuyPrice()) {
            formatString.append("Last GE Buy Price: ")
                    .append(lastBuyPriceString)
                    .append(" GP</br>");
        }
        if (config.showLastSellPrice()) {
            formatString.append("Last GE Sell Price: ")
                    .append(lastSellPriceString)
                    .append(" GP</br>");
        }
        if (config.showLastBuyTime()) {
            formatString.append("Last GE Buy Time: ")
                    .append(TimeUtils.timeAgo(lastBuyTime))
                    .append("</br>");
        }
        if (config.showLastSellTime()) {
            formatString.append("Last GE Sell Time: ")
                    .append(TimeUtils.timeAgo(lastSellTime))
                    .append("</br>");
        }

        if (formatString.length() == 0) return null;
        return ColorUtil.prependColorTag(formatString.toString(), Color.WHITE);
    }

    private String buildSummarySection(GEItemPriceData priceData, int fallbackPrice, ItemComposition comp) {
        int usedPrice = fallbackPrice;
        boolean itemIsTradeable = comp.isTradeable();

        if (priceData != null && itemIsTradeable) {
            usedPrice = Math.min(priceData.getLow(), priceData.getHigh());
        }

        StringBuilder formatString = new StringBuilder();

        long priceQuantity = (long) usedPrice * itemQuantity;
        long haPriceQuantity = (long) comp.getHaPrice() * itemQuantity;
        int itemId = comp.getId();
        itemId = itemManager.canonicalize(itemId);

        if (itemId == ItemID.COINS || itemId == ItemID.PLATINUM) {
            formatString.append(NumberFormatUtils.formatNumber(priceQuantity))
                    .append(" GP</br>");
        } else if (priceQuantity > 0 && itemIsTradeable) {
            formatString.append("GE: ")
                    .append(NumberFormatUtils.abbreviateNumber(priceQuantity))
                    .append(" GP");

            appendSingularPrice(formatString, usedPrice);
            formatString.append("</br>");
        }

        if (haPriceQuantity > 0) {
            formatString.append("HA: ")
                    .append(NumberFormatUtils.abbreviateNumber(haPriceQuantity))
                    .append(" GP");

            appendSingularPrice(formatString, comp.getHaPrice());
        }

        if (formatString.length() == 0) return null;
        return ColorUtil.prependColorTag(formatString.toString(), Color.WHITE);
    }

    private boolean shouldEnableTooltip(MenuEntry menuEntry) {
        int itemId = getCanonIdWithGroundCheck(menuEntry);

        if (isGroundItem(menuEntry)) {
            return true;
        }
        if (itemId < 1) return false;

        Widget widget = menuEntry.getWidget();

        return widget != null &&
                (WidgetInfo.INVENTORY.getId() == widget.getId()
                        || WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId() == widget.getId()
                        || WidgetInfo.BANK_ITEM_CONTAINER.getId() == widget.getId()
                        || WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER.getId() == widget.getId()
                );
    }

    private boolean isGroundItem(MenuEntry entry) {
        MenuAction action = entry.getType();
        return action == MenuAction.EXAMINE_ITEM_GROUND;
    }


    private void appendSingularPrice(StringBuilder sb, int price) {
        if (itemQuantity > 1) {
            sb.append(" (")
                    .append(NumberFormatUtils.abbreviateNumber(price))
                    .append(" ea)");
        }
    }

    private int getGroundItemQuantity(MenuEntry entry) {
        int sceneX = entry.getParam0();
        int sceneY = entry.getParam1();

        Tile tile = client.getScene().getTiles()[client.getPlane()][sceneX][sceneY];
        if (tile == null) return 1;

        int itemId = itemManager.canonicalize(entry.getIdentifier());

        for (TileItem item : tile.getGroundItems()) {
            if (itemManager.canonicalize(item.getId()) == itemId) {
                return item.getQuantity();
            }
        }
        return 1;
    }

    private int getCanonIdWithGroundCheck(MenuEntry entry) {
        return itemManager.canonicalize(isGroundItem(entry) ? entry.getIdentifier() : entry.getItemId());
    }
}
