package com.trademaster;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("trademaster")
public interface TradeMasterConfig extends Config {
    @ConfigItem(
            position = 6,
            keyName = "greeting",
            name = "Welcome Greeting",
            description = "The message to show to the user when they login"
    )
    default String greeting() {
        return "Hello";
    }

    //    Testing some stuff from here on
    @ConfigItem(
            position = 1,
            keyName = "myCheckbox",
            name = "Checkbox",
            description = "Description Hover Text"
    )
    default boolean myCheckbox() {
        return true;
    }

    @ConfigItem(
            position = 3,
            keyName = "mySpinner",
            name = "Spinner",
            description = "Description Hover Text"
    )
    default int mySpinner() {
        return 0;
    }

    @ConfigItem(
            position = 2,
            keyName = "mySpinnerX2",
            name = "SpinnerX2",
            description = "Description Hover Text"
    )
    default Dimension mySpinnerX2() {
        return new Dimension(3, 5);
    }

    @ConfigItem(
            position = 4,
            keyName = "myColor",
            name = "Colorpicker",
            description = "Description Hover Text"
    )
    default Color myColor() {
        return new Color(255, 255, 0);
    }

    @ConfigItem(
            position = 5,
            keyName = "myEnum",
            name = "Enum",
            description = "Description Hover Text"
    )
    default TestEnum myEnum() {
        return TestEnum.SECOND;
    }

    enum TestEnum {FIRST, SECOND, THIRD}

}

