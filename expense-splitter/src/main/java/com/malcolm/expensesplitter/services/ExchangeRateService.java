package com.malcolm.expensesplitter.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud-native Exchange Rate Service.
 * Fetches live rates from the Currency API and maintains an in-memory cache.
 * All local file system caching (offline functionality) has been removed.
 */
@Service
public class ExchangeRateService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, BigDecimal> rateCache = new ConcurrentHashMap<>();
    private final java.util.Set<String> attemptedCurrenciesToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastFetchDate;
    private final Map<String, String> currencyNamesCache = new ConcurrentHashMap<>();

    public ExchangeRateService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.lastFetchDate = LocalDate.now().minusDays(1);
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
                }
            }
        } catch (Exception e) {
            System.err.println("Cloud-Service: Failed to fetch live currency names: " + e.getMessage());
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

        if (LocalDate.now().isAfter(lastFetchDate)) {
            boolean didFetch = fetchAndCache(from);
            if (didFetch) {
                lastFetchDate = LocalDate.now();
            }
        }

        if (rateCache.containsKey(cacheKey)) {
            return rateCache.get(cacheKey);
        }

        if (!attemptedCurrenciesToday.contains(from)) {
            fetchAndCache(from);
            attemptedCurrenciesToday.add(from);
        }

        return rateCache.getOrDefault(cacheKey, BigDecimal.ONE);
    }

    private boolean fetchAndCache(String from) {
        String primaryUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/" + from + ".json";
        String fallbackUrl = "https://latest.currency-api.pages.dev/v1/currencies/" + from + ".json";

        if (tryFetch(primaryUrl, from)) return true;
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
                            rateCache.put(toCurrency + "_" + from, BigDecimal.ONE.divide(rate, 10, java.math.RoundingMode.HALF_UP));
                        }
                    });
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
}
