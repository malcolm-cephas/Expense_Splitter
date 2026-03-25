package com.malcolm.expensesplitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class AppConfig {

    @Value("${app.currency.code:INR}")
    private String systemBaseCode;

    @Value("${app.currency.symbol:\u20B9}")
    private String systemBaseSymbol;

    private String userPreferredCode;

    private static final Map<String, String> SYMBOLS = Map.ofEntries(
            Map.entry("INR", "\u20B9"),
            Map.entry("USD", "$"),
            Map.entry("EUR", "\u20AC"),
            Map.entry("GBP", "\u00A3"),
            Map.entry("JPY", "\u00A5"),
            Map.entry("CAD", "C$"),
            Map.entry("AUD", "A$"),
            Map.entry("AED", "dh"),
            Map.entry("BTC", "\u20BF"),
            Map.entry("ETH", "\u039E"),
            Map.entry("SOL", "S"),
            Map.entry("USDT", "\u20AE"));

    @org.springframework.context.annotation.Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    public String getCurrencyCode() {
        // Default to user preference if set, else system code
        return userPreferredCode != null ? userPreferredCode : systemBaseCode;
    }

    /**
     * Use this for core calculation base (always respect system-wide default)
     */
    public String getSystemBaseCode() {
        return systemBaseCode != null ? systemBaseCode : "INR";
    }

    public void setCurrencyCode(String currencyCode) {
        this.userPreferredCode = currencyCode;
    }

    public String getCurrencySymbol() {
        return getSymbol(getCurrencyCode());
    }

    public String getSystemBaseSymbol() {
        return systemBaseSymbol != null ? systemBaseSymbol : "\u20B9";
    }

    public String getSymbol(String code) {
        if (code == null)
            return systemBaseSymbol;
        return SYMBOLS.getOrDefault(code.toUpperCase(), code);
    }
}
