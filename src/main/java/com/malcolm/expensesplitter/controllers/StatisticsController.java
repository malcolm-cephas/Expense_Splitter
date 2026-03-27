package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.models.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.malcolm.expensesplitter.services.ExchangeRateService;
import com.malcolm.expensesplitter.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
@Scope("prototype")
public class StatisticsController {

    @FXML
    private PieChart categoryPieChart;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private AppConfig appConfig;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setData(List<Expense> expenses) {
        String baseCurrency = appConfig.getCurrencyCode();
        Map<String, BigDecimal> totalsByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : "Other",
                        Collectors.mapping(e -> {
                            BigDecimal amount = e.getAmount();
                            String currency = e.getCurrency();
                            if (currency != null && !currency.isEmpty() && !currency.equalsIgnoreCase(baseCurrency)) {
                                BigDecimal rate = exchangeRateService.getExchangeRate(currency, baseCurrency);
                                return amount.multiply(rate);
                            }
                            return amount;
                        }, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                totalsByCategory.entrySet().stream()
                        .map(entry -> new PieChart.Data(entry.getKey() + " (" + appConfig.getSymbol(baseCurrency)
                                 + appConfig.formatAmount(entry.getValue().setScale(0, java.math.RoundingMode.CEILING), baseCurrency) + ")",
                                 entry.getValue().doubleValue()))
                        .collect(Collectors.toList()));

        categoryPieChart.setData(pieChartData);
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
