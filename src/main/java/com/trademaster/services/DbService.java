package com.trademaster.services;

import com.trademaster.db.DbManager;
import com.trademaster.db.models.PlayerData;
import com.trademaster.db.models.WealthData;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class DbService {
    private DbManager dbManager;

    public void create(String playerName, WealthData wealthData) {
        dbManager = new DbManager(playerName, wealthData);
        log.info("DBManager created");
    }

    public DbManager get() {
        return dbManager;
    }

    public void close() {
        if (dbManager != null) {
            dbManager.writeToFile();
            dbManager = null;
            log.info("DbManager closed");
        }
    }

    public PlayerData getFallBackData() {
        DbManager dm = new DbManager();
        if (!dm.dbFileExists()) {
            return null;
        }
        return dm.getDbFileData();
    }
}
