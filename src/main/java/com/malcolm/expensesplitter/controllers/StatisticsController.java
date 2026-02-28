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

@Controller
@Scope("prototype")
public class StatisticsController {

    @FXML
    private PieChart categoryPieChart;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setData(List<Expense> expenses) {
        Map<String, BigDecimal> totalsByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : "Other",
                        Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                totalsByCategory.entrySet().stream()
                        .map(entry -> new PieChart.Data(entry.getKey() + " (₹"
                                + entry.getValue().setScale(0, java.math.RoundingMode.CEILING) + ")",
                                entry.getValue().doubleValue()))
                        .collect(Collectors.toList()));

        categoryPieChart.setData(pieChartData);
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
