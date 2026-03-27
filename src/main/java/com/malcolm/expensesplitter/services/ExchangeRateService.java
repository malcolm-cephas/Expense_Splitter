package com.malcolm.expensesplitter.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, BigDecimal> rateCache = new ConcurrentHashMap<>();
    private final java.util.Set<String> attemptedCurrenciesToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastFetchDate;
    private final File cacheFile = new File("exchange_rates.json");
    private final File currencyNamesFile = new File("currency_names.json");
    private final Map<String, String> currencyNamesCache = new ConcurrentHashMap<>();

    public ExchangeRateService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        loadCacheFromFile();
        loadCurrencyNamesFromFile();
    }

    private void loadCacheFromFile() {
        if (cacheFile.exists()) {
            try {
                CacheData data = objectMapper.readValue(cacheFile, CacheData.class);
                if (data != null && data.rates != null) {
                    rateCache.putAll(data.rates);
                    if (data.lastFetchDate != null && !data.lastFetchDate.isEmpty()) {
                        lastFetchDate = LocalDate.parse(data.lastFetchDate);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load exchange rates cache: " + e.getMessage());
            }
        }
        if (lastFetchDate == null) {
            lastFetchDate = LocalDate.now().minusDays(1);
        }
    }

    private void saveCacheToFile() {
        try {
            CacheData data = new CacheData();
            data.rates = rateCache;
            data.lastFetchDate = LocalDate.now().toString();
            objectMapper.writeValue(cacheFile, data);
        } catch (IOException e) {
            System.err.println("Failed to save exchange rates cache: " + e.getMessage());
        }
    }

    private void loadCurrencyNamesFromFile() {
        if (currencyNamesFile.exists()) {
            try {
                Map<String, String> names = objectMapper.readValue(currencyNamesFile, new TypeReference<Map<String, String>>() {});
                if (names != null) {
                    currencyNamesCache.putAll(names);
                }
            } catch (Exception e) {
                System.err.println("Failed to load currency names cache: " + e.getMessage());
            }
        }
        if (currencyNamesCache.isEmpty()) {
            fetchCurrencyNames();
        }
    }

    private void saveCurrencyNamesToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(currencyNamesFile, currencyNamesCache);
        } catch (IOException e) {
            System.err.println("Failed to save currency names: " + e.getMessage());
        }
    }

    public void fetchCurrencyNames() {
        String url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, String> names = objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
                if (names != null) {
                    currencyNamesCache.putAll(names);
                    saveCurrencyNamesToFile();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch currency names: " + e.getMessage());
        }
    }

    public Map<String, String> getAllCurrencies() {
        if (currencyNamesCache.isEmpty()) {
            fetchCurrencyNames();
        }
        return currencyNamesCache;
    }

    public String getCurrencyName(String code) {
        return currencyNamesCache.getOrDefault(code.toLowerCase(), code.toUpperCase());
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }

        String from = fromCurrency.toLowerCase();
        String to = toCurrency.toLowerCase();
        String cacheKey = from + "_" + to;

        boolean didFetch = false;
        if (LocalDate.now().isAfter(lastFetchDate)) {
            // Attempt to fetch new rates for base currency
            didFetch = fetchAndCache(from);
            if (didFetch) {
                lastFetchDate = LocalDate.now();
                saveCacheToFile();
            }
        }

        if (rateCache.containsKey(cacheKey)) {
            return rateCache.get(cacheKey);
        }

        // If not in cache and we haven't attempted this currency yet today
        if (!attemptedCurrenciesToday.contains(from)) {
            didFetch = fetchAndCache(from);
            attemptedCurrenciesToday.add(from);
            if (didFetch) {
                lastFetchDate = LocalDate.now();
                saveCacheToFile();
            }
        }

        return rateCache.getOrDefault(cacheKey, BigDecimal.ONE);
    }

    private boolean fetchAndCache(String from) {
        String primaryUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/" + from
                + ".json";
        String fallbackUrl = "https://latest.currency-api.pages.dev/v1/currencies/" + from + ".json";

        if (tryFetch(primaryUrl, from)) {
            return true;
        }

        System.out.println("Primary currency API failed, trying fallback...");
        return tryFetch(fallbackUrl, from);
    }

    private boolean tryFetch(String url, String from) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode rates = root.get(from);
                if (rates != null) {
                    rates.fields().forEachRemaining(entry -> {
                        String toCurrency = entry.getKey();
                        BigDecimal rate = new BigDecimal(entry.getValue().asText());
                        rateCache.put(from + "_" + toCurrency, rate);

                        if (rate.compareTo(BigDecimal.ZERO) != 0) {
                            rateCache.put(toCurrency + "_" + from,
                                    BigDecimal.ONE.divide(rate, 10, java.math.RoundingMode.HALF_UP));
                        }
                    });
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch from " + url + ": " + e.getMessage());
        }
        return false;
    }

    public static class CacheData {
        public Map<String, BigDecimal> rates;
        public String lastFetchDate;
    }
}
