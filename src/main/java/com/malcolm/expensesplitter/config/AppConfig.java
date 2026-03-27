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

    /**
     * Formats an amount with commas based on the currency's grouping system.
     */
    public String formatAmount(java.math.BigDecimal amount) {
        return formatAmount(amount, getCurrencyCode());
    }

    public String formatAmount(java.math.BigDecimal amount, String currencyCode) {
        if (amount == null)
            return "0";
        
        String code = (currencyCode != null) ? currencyCode.toUpperCase() : getCurrencyCode();
        java.math.BigDecimal raw = amount.stripTrailingZeros();
        String plain = raw.toPlainString();
        
        String integerPart;
        String decimalPart = "";
        int dotIndex = plain.indexOf('.');
        if (dotIndex >= 0) {
            integerPart = plain.substring(0, dotIndex);
            decimalPart = plain.substring(dotIndex);
            if (decimalPart.length() > 3) {
                decimalPart = decimalPart.substring(0, 3);
            }
        } else {
            integerPart = plain;
        }

        boolean negative = integerPart.startsWith("-");
        if (negative) {
            integerPart = integerPart.substring(1);
        }

        String formattedInteger;
        if (isIndian(code)) {
            formattedInteger = formatIndianGrouping(integerPart);
        } else if (isMyriad(code)) {
            formattedInteger = formatGrouping(integerPart, 4);
        } else {
            formattedInteger = formatGrouping(integerPart, 3);
        }

        return (negative ? "-" : "") + formattedInteger + decimalPart;
    }

    private boolean isIndian(String code) {
        return java.util.Set.of("INR", "PKR", "BDT", "NPR", "LKR").contains(code);
    }

    private boolean isMyriad(String code) {
        return java.util.Set.of("JPY", "CNY", "KRW", "TWD").contains(code);
    }

    private String formatGrouping(String integerPart, int groupSize) {
        if (integerPart.length() <= groupSize) return integerPart;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = integerPart.length() - 1; i >= 0; i--) {
            if (count > 0 && count % groupSize == 0) sb.append(",");
            sb.append(integerPart.charAt(i));
            count++;
        }
        return sb.reverse().toString();
    }

    private String formatIndianGrouping(String integerPart) {
        if (integerPart.length() <= 3) return integerPart;
        String lastThree = integerPart.substring(integerPart.length() - 3);
        String rest = integerPart.substring(0, integerPart.length() - 3);
        
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = rest.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 2 == 0) sb.append(",");
            sb.append(rest.charAt(i));
            count++;
        }
        return sb.reverse().toString() + "," + lastThree;
    }
}
