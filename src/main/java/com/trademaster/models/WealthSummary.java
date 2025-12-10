package com.trademaster.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WealthSummary {
    private String bank;
    private String inventory;
    private String total;
    private String totalAbbreviated;

    public WealthSummary(String bank, String inventory, String total, String totalAbbreviated) {
        this.bank = bank + " GP";
        this.inventory = inventory + " GP";
        this.total = total + " GP";
        this.totalAbbreviated = totalAbbreviated + " GP";
    }
}
