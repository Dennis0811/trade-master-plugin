package com.trademaster.models;

import com.trademaster.db.models.PlayerWealthData;
import com.trademaster.db.models.WealthData;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HomeModel {
    private long bankWealth;
    private long inventoryWealth;
    private long geWealth;

    public HomeModel() {
        this.bankWealth = 0;
        this.inventoryWealth = 0;
        this.geWealth = 0;
    }

    public HomeModel(PlayerWealthData playerWealthData) {
        WealthData wealthData = playerWealthData.getWealthData();
        this.bankWealth = wealthData.getBankWealth();
        this.inventoryWealth = wealthData.getInventoryWealth();
        this.geWealth = wealthData.getGeWealth();
    }
    
    public long getPlayerWealth() {
        return bankWealth + inventoryWealth + geWealth;
    }

    public void loadWealthDataFromFile(PlayerWealthData playerWealthData) {
        WealthData wealthData = playerWealthData.getWealthData();

        this.bankWealth = wealthData.getBankWealth();
        this.inventoryWealth = wealthData.getInventoryWealth();
        this.geWealth = wealthData.getGeWealth();
    }
}