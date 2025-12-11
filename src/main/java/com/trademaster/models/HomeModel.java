package com.trademaster.models;

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

    public HomeModel(long bankWealth, long inventoryWealth, long geWealth) {
        this.bankWealth = bankWealth;
        this.inventoryWealth = inventoryWealth;
        this.geWealth = geWealth;
    }

    public long getPlayerWealth() {
        return bankWealth + inventoryWealth + geWealth;
    }
}