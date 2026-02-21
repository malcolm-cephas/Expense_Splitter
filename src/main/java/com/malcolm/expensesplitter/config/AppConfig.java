package com.malcolm.expensesplitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {

    @Value("${app.currency.code:INR}")
    private String currencyCode;

    @Value("${app.currency.symbol:\u20B9}")
    private String currencySymbol;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }
}
