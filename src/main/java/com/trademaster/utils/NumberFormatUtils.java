package com.trademaster.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class NumberFormatUtils {
    public static String formatNumber(long value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    public static String abbreviateNumber(long value) {
        return abbreviateNumber(value, 1_000);
    }

    public static String abbreviateNumber(long value, long minNumber) {
        if (value < minNumber) {
            return formatNumber(value);
        }

        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "Se"};
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
