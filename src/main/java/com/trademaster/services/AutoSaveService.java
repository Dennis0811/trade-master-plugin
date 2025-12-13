package com.trademaster.services;

import com.trademaster.TradeMasterConfig;
import com.trademaster.db.DbManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class AutoSaveService {
    @Inject
    private TradeMasterConfig config;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private Client client;

    private DbManager dbManager;
    private ScheduledFuture<?> saveTask;

    public void start(DbManager dbManager) {
        this.dbManager = dbManager;
        scheduleIfEnabled();
    }

    public void stop() {
        cancel();
    }

    public void reschedule() {
        scheduleIfEnabled();
    }


    private void scheduleIfEnabled() {
        cancel();

        if (!config.autoSaveEnabled()) {
            return;
        }

        int intervalMinutes = config.autoSaveInterval();

        saveTask = executor.scheduleAtFixedRate(
                this::autoSave,
                intervalMinutes,
                intervalMinutes,
                TimeUnit.SECONDS
        );
    }

    private void cancel() {
        if (saveTask != null) {
            saveTask.cancel(false);
            saveTask = null;
        }
    }

    private void autoSave() {
        try {
            if (dbManager == null) {
                return;
            }

            if (client.getGameState() != GameState.LOGGED_IN) {
                return;
            }

            dbManager.writeToFile();
        } catch (Exception e) {
            log.warn("AutoSaveService failed to save", e);
        }
    }
}
