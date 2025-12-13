package com.trademaster.services;

import com.trademaster.controllers.HomeController;
import com.trademaster.db.models.WealthData;
import com.trademaster.models.HomeModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class WealthDataService {
    @Inject
    private ItemManager itemManager;

    @Getter
    private final WealthData wealthData = new WealthData();
    private HomeController controller;
    private HomeModel model;

    public void attachController(HomeController controller) {
        this.controller = controller;
    }

    public void attachModel(HomeModel model) {
        this.model = model;
    }

    public void updateBank(ItemContainer container) {
        long value = calculate(container);
        wealthData.setBankWealth(value);
        model.setBankWealth(value);
        controller.refresh();
    }

    public void updateInventory(ItemContainer container) {
        long value = calculate(container);
        wealthData.setInventoryWealth(value);
        model.setInventoryWealth(value);
        controller.refresh();
    }

    public void updateGe(GrandExchangeOffer[] offers) {
        long value = 0;

        if (offers == null) {
            return;
        }

        for (GrandExchangeOffer offer : offers) {
            if (offer != null) {
                value += (long) offer.getPrice() * offer.getTotalQuantity();
            }
        }

        wealthData.setGeWealth(value);
        model.setGeWealth(value);
        controller.refresh();
    }

    private long calculate(ItemContainer container) {
        long total = 0;
        for (Item item : container.getItems()) {
            if (item.getId() > 0) {
                total += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
            }
        }
        return total;
    }


}
