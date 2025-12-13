package com.trademaster.db;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trademaster.db.models.PlayerData;
import com.trademaster.db.models.TradeData;
import com.trademaster.db.models.WealthData;
import com.trademaster.types.OfferTypes;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.*;

@Slf4j
public class DbManager {
    private final String FOLDER_NAME = "trademaster";
    private final File DB_DIR = new File(RuneLite.RUNELITE_DIR, FOLDER_NAME);
    private final File DB_FILE;

    private WealthData wealthData;
    private TradeData[] currentOfferData;
    private TradeData[] tradeData;


    public DbManager() {
        // Takes the first json file and displays wealth data with it
        File[] files = DB_DIR.listFiles((d, name) -> name.endsWith(".json"));

        if (files != null && files.length > 0) {
            this.DB_FILE = files[0];
        } else {
            this.DB_FILE = null; // No DB yet
        }
    }

    public DbManager(String playerName, PlayerData playerData) {
        this.DB_FILE = new File(DB_DIR, playerName + ".json");
        this.wealthData = playerData.getWealthData();
        this.currentOfferData = playerData.getCurrentGeOfferData();
        this.tradeData = playerData.getPastTradeData();
    }

    public DbManager(String playerName, WealthData wealthData) {
        this.DB_FILE = new File(DB_DIR, playerName + ".json");
        this.wealthData = wealthData;
    }

    public DbManager(String playerName, TradeData[] tradeData, boolean isPastTradeData) {
        this.DB_FILE = new File(DB_DIR, playerName + ".json");

        if (isPastTradeData) {
            this.tradeData = tradeData;

        } else {
            this.currentOfferData = tradeData;
        }
    }

    public boolean dbFileExists() {
        return DB_FILE != null && DB_FILE.exists();
    }

    public PlayerData getDbFileData() {
        try (Reader reader = new FileReader(DB_FILE)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, PlayerData.class);
        } catch (IOException e) {
            log.warn("Couldn't read {} - {}", DB_FILE.getName(), e.getMessage());
        }
        return null;
    }

    public void writeToFile() {
        createFolder();

        JsonObject dbFile = new JsonObject();

        // Wealth
        JsonObject wealth = createWealthJson();
        dbFile.add("wealth", wealth);

        // Current GE Offers
        JsonArray currentOffers = new JsonArray();
        JsonObject offer1 = createTradeJson();
        currentOffers.add(offer1);
        dbFile.add("currentGeOffers", currentOffers);

        // Past GE Trades
        JsonArray trades = new JsonArray();
        JsonObject trade1 = createTradeJson();
        trades.add(trade1);

        JsonObject trade2 = createTradeJson();
        trades.add(trade2);
        dbFile.add("geTrades", trades);


        try (Writer writer = new FileWriter(this.DB_FILE)) {
            log.info("Creating {} at {}", this.DB_FILE.getName(), this.DB_FILE.getPath());

            Gson gson = new Gson();
            gson.toJson(dbFile, writer);
        } catch (IOException e) {
            log.warn("Couldn't create DB file {}", e.getMessage());
        }
    }


    /**
     * Creates a Json Object fills it with data with WealthData.
     *
     * @return Returns the Json Object
     */
    private JsonObject createWealthJson() {
        JsonObject jo = new JsonObject();
        jo.addProperty("bank", wealthData.getBankWealth());
        jo.addProperty("inventory", wealthData.getInventoryWealth());
        jo.addProperty("ge", wealthData.getGeWealth());
        return jo;
    }

    /**
     * Creates a Json Object fills it with data with TradeData.
     *
     * @return Returns the Json Object
     */
    private JsonObject createTradeJson() {
        JsonObject jo = new JsonObject();
        jo.addProperty("itemId", 123);
        jo.addProperty("quantity", 123);
        jo.addProperty("price", 123);
        jo.addProperty("offerType", OfferTypes.BUY.name());
        jo.addProperty("startTime", 123);
        jo.addProperty("endTime", 123);
        return jo;
    }

    public void exportAsCsv() {
        // export a csv file created from my json file
    }

    /**
     * Creates a folder to house all data / db files.
     */
    private void createFolder() {
        if (!DB_DIR.exists()) {
            log.info("DB directory is being created.");
            if (!DB_DIR.mkdir()) {
                log.warn("DB directory couldn't be created!");
            }
        } else {
            log.info("DB directory already exists - {}", DB_DIR);
        }
    }
}
