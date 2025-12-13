package com.trademaster;

import com.trademaster.types.AbbreviateThresholdTypes;
import net.runelite.client.config.*;

@ConfigGroup("trademaster")
public interface TradeMasterConfig extends Config {
    @ConfigSection(
            position = 1,
            name = "Auto Save",
            description = "Automatically saves plugin data in whatever interval is set."
    )
    String autoSaveSection = "autoSaveSection";

    @ConfigItem(
            section = autoSaveSection,
            position = 1,
            keyName = "autoSaveInterval",
            name = "Auto Save Interval",
            description = "How often plugin data is saved (in minutes)."
    )
    @Units(Units.MINUTES)
    @Range(min = 1)
    default int autoSaveInterval() {
        return 5;
    }

    @ConfigItem(
            position = 2,
            section = autoSaveSection,
            keyName = "autoSaveEnabled",
            name = "Enable auto save",
            description = "Turns on/ off the auto saving feature."
    )
    default boolean autoSaveEnabled() {
        return true;
    }

    @ConfigSection(
            name = "Abbreviate numbers",
            description = "Turn big numbers into short forms like 1.2 M or 2.5 B.",
            position = 2
    )
    String shortenSection = "shortenSection";

    @ConfigItem(
            section = shortenSection,
            position = 1,
            keyName = "abbreviateThreshold",
            name = "Abbreviate above",
            description = "Show abbreviated format (1.2 M) for numbers larger than this value."
    )
    default AbbreviateThresholdTypes abbreviateThreshold() {
        return AbbreviateThresholdTypes.TRILLION;
    }

    @ConfigItem(
            section = shortenSection,
            position = 2,
            keyName = "abbreviateGpTotalEnabled",
            name = "Abbreviate GP total",
            description = "Turn on abbreviated GP display."
    )
    default boolean abbreviateGpTotalEnabled() {
        return true;
    }

    @ConfigItem(
            section = shortenSection,
            position = 3,
            keyName = "abbreviateHoverGpTotalEnabled",
            name = "Abbreviate tooltip GP total",
            description = "Abbreviates GP total value in tooltip."
    )
    default boolean abbreviateHoverGpTotalEnabled() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 4,
            keyName = "abbreviateHoverBankEnabled",
            name = "Abbreviate tooltip bank value",
            description = "Abbreviates bank value in tooltip."
    )
    default boolean abbreviateHoverBankEnabled() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 5,
            keyName = "abbreviateHoverInventoryEnabled",
            name = "Abbreviate tooltip inventory value",
            description = "Abbreviates inventory value in tooltip."
    )
    default boolean abbreviateHoverInventoryEnabled() {
        return false;
    }

    @ConfigItem(
            section = shortenSection,
            position = 6,
            keyName = "abbreviateHoverGeEnabled",
            name = "Abbreviate tooltip GE value",
            description = "Abbreviates Grand Exchange value in tooltip."
    )
    default boolean abbreviateHoverGeEnabled() {
        return false;
    }
}

