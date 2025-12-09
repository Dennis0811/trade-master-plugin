package com.trademaster;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TradeMasterPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(TradeMasterPlugin.class);
        RuneLite.main(args);
    }
}