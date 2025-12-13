package com.trademaster.services;

import com.google.gson.Gson;
import com.trademaster.services.models.CachedPrice;
import com.trademaster.services.models.GEItemPriceData;
import com.trademaster.services.models.GEPriceResponse;
import com.trademaster.types.TimestepType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Singleton
public class GEPriceService {
    private final static String USER_AGENT_VALUE = "Trade Master trademaster@gmail.com";
    private final static String BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
    private final static String LATEST = "/latest";
    private final static String MAPPING = "/mapping";
    private final static String TIMESERIES = "/timeseries";
    private final static String ID_QUERY_PARAMETER = "id";
    private static final long CACHE_LIFETIME = 60 * 1000;

    private TimestepType timestepType;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Map<Integer, CachedPrice> priceCache = new HashMap<>();


    public GEPriceService() {
        this.httpClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
    }

    public GEItemPriceData getPrice(int itemId) {
        CachedPrice cached = priceCache.get(itemId);
        long now = System.currentTimeMillis();

        if (cached != null && (now - cached.getFetchedAt()) < CACHE_LIFETIME) {
            return cached.getPrice();
        }

        GEItemPriceData fresh = fetchPrice(itemId);
        if (fresh != null) {
            priceCache.put(itemId, new CachedPrice(fresh, now));
        }

        return fresh;
    }

    private GEItemPriceData fetchPrice(int itemId) {
        log.debug("FETCHING NEW PRICES");
        String url = String.format("%s%s?%s=%d", BASE_URL, LATEST, ID_QUERY_PARAMETER, itemId);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT_VALUE)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            String body = response.body().string();
            GEPriceResponse priceResponse = gson.fromJson(body, GEPriceResponse.class);

            return priceResponse.getData().get(String.valueOf(itemId));
        } catch (Exception e) {
            log.warn("Failed to fetch item price data {}", e.getMessage());
            return null;
        }

    }
}

