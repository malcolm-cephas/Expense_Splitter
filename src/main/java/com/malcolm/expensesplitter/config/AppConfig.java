package com.malcolm.expensesplitter.config;

import org.springframework.stereotype.Component;

@Component
public class AppConfig {

    private String currencyCode = "INR";
    private String currencySymbol = "\u20B9";

    private static final java.util.Map<String, String> SYMBOLS = java.util.Map.of(
            "INR", "\u20B9",
            "USD", "$",
            "EUR", "\u20AC",
            "GBP", "\u00A3",
            "JPY", "\u00A5",
            "CAD", "C$",
            "AUD", "A$",
            "AED", "dh");

    @org.springframework.context.annotation.Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
        this.currencySymbol = SYMBOLS.getOrDefault(currencyCode, currencyCode);
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public String getSymbol(String code) {
        if (code == null)
            return currencySymbol;
        return SYMBOLS.getOrDefault(code.toUpperCase(), code);
    }
}
