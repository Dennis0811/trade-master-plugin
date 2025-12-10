package com.trademaster.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
public class HomeModel {
    private long bankWealth;
    private long inventoryWealth;

    public HomeModel(long bankWealth, long inventoryWealth) {
        this.bankWealth = bankWealth;
        this.inventoryWealth = inventoryWealth;
    }

    public long getPlayerWealth() {
        return bankWealth + inventoryWealth;
    }

}
