package com.trademaster.types;

import lombok.Getter;

public enum AbbreviateThresholdTypes {
    THOUSAND((long) Math.pow(10, 4), "1 Thousand"),
    MILLION((long) Math.pow(10, 6), "1 Million"),
    BILLION((long) Math.pow(10, 9), "1 Billion"),
    TRILLION((long) Math.pow(10, 12), "1 Trillion"),
    QUADRILLION((long) Math.pow(10, 15), "1 Quadrillion"),
    QUINTILLION((long) Math.pow(10, 18), "1 Quintillion"),
    SEXTILLION((long) Math.pow(10, 21), "1 Sextillion"),
    MAXIMUM(Long.MAX_VALUE, "Maximum");


    @Getter
    private final long value;
    private final String displayedValue;

    AbbreviateThresholdTypes(long value, String displayedValue) {
        this.value = value;
        this.displayedValue = displayedValue;
    }

    @Override
    public String toString() {
        return displayedValue;
    }

}
