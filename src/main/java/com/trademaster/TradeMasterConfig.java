package com.trademaster;

import com.trademaster.types.AbbreviateThresholdTypes;
import net.runelite.client.config.*;

@ConfigGroup("trademaster")
public interface TradeMasterConfig extends Config {
    @ConfigSection(
            name = "Abbreviate numbers",
            description = "Turn big numbers into short forms like 1.2 M or 2.5 B.",
            position = 1
    )
    String shortenSection = "shortenSection";

    @ConfigItem(
            section = shortenSection,
            position = 1,
            keyName = "abbreviateThreshold",
            name = "Abbreviate numbers above",
            description = "Show abbreviated format (1.2 M) for numbers larger than this value."
    )
    default AbbreviateThresholdTypes abbreviateThreshold() {
        return AbbreviateThresholdTypes.TRILLION;
    }

    @ConfigItem(
            section = shortenSection,
            position = 2,
            keyName = "abbreviateGpTotal",
            name = "Abbreviate GP total",
            description = "Turn on abbreviated GP display."
    )
    default boolean abbreviateGpTotal() {
        return true;
    }

    @ConfigItem(
            section = shortenSection,
            position = 3,
            keyName = "abbreviateHoverGpTotal",
            name = "Abbreviate tooltip GP total",
            description = "Abbreviates GP total value in tooltip."
    )
    default boolean abbreviateHoverGpTotal() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 4,
            keyName = "abbreviateHoverBank",
            name = "Abbreviate tooltip bank value",
            description = "Abbreviates bank value in tooltip."
    )
    default boolean abbreviateHoverBank() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 5,
            keyName = "abbreviateHoverInventory",
            name = "Abbreviate tooltip inventory value",
            description = "Abbreviates inventory value in tooltip."
    )
    default boolean abbreviateHoverInventory() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 6,
            keyName = "abbreviateHoverGe",
            name = "Abbreviate tooltip GE value",
            description = "Abbreviates Grand Exchange value in tooltip."
    )
    default boolean abbreviateHoverGe() {
        return false;
    }
}

