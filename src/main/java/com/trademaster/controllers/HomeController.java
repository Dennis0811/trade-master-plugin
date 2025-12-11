package com.trademaster.controllers;

import com.trademaster.models.HomeModel;
import com.trademaster.views.home.HomeView;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.text.NumberFormat;

@Slf4j
public class HomeController {
    private final HomeModel model;

    @Setter
    private HomeView view;

    public HomeController(HomeModel model) {
        this.model = model;
    }

    public void refresh() {
        updateWealthDisplay();
    }

    public void updateWealthDisplay() {
        long total = model.getPlayerWealth();
        long bank = model.getBankWealth();
        long inventory = model.getInventoryWealth();
        long ge = model.getGeWealth();

        view.setWealthText(
                formatNumber(bank),
                formatNumber(inventory),
                formatNumber(ge),
                formatNumber(total),
                abbreviateNumber(total)
        );
    }


    public String formatNumber(long value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    public String abbreviateNumber(long value) {
        final long minNumber = 1_000_000_000_000L;

        if (value < minNumber) {
            return formatNumber(value);
        }

        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "Oc"};
        int suffixIndex = 0;
        double dividedValue = value;

        while (dividedValue >= 1000 && suffixIndex < suffixes.length - 1) {
            dividedValue /= 1000;
            suffixIndex++;
        }

        // Use DecimalFormat to remove trailing zeros, max 3 decimals
        DecimalFormat df = new DecimalFormat("#0.###");
        return df.format(dividedValue) + " " + suffixes[suffixIndex];
    }

}
