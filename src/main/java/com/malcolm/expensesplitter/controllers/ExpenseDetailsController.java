package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.services.ExpenseService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Controller
public class ExpenseDetailsController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AppConfig appConfig;

    @FXML
    private Label expenseInfoLabel;

    @FXML
    private Label expenseDateLabel;

    @FXML
    private VBox splitsContainer;

    private Expense currentExpense;
    private Stage dialogStage;
    private boolean changed = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setExpense(Expense expense) {
        this.currentExpense = expense;
        String payerText;
        if (expense.getPayments().size() > 1) {
            payerText = "Multiple Payers";
        } else {
            payerText = "Paid by " + (expense.getPaidBy() != null ? expense.getPaidBy().getName() : "Unknown");
        }

        String currency = expense.getCurrency() != null && !expense.getCurrency().isEmpty() ? expense.getCurrency()
                : "INR";
        String symbol = appConfig.getSymbol(currency);
        expenseInfoLabel.setText(
                expense.getDescription() + " - " + symbol + expense.getAmount() + " " + currency
                        + " ("
                        + payerText + ")");

        if (expense.getExpenseDate() != null) {
            expenseDateLabel.setText(expense.getExpenseDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        } else {
            expenseDateLabel.setText("");
        }

        loadSplits();
    }

    private void loadSplits() {
        splitsContainer.getChildren().clear();
        for (ExpenseSplit split : currentExpense.getSplits()) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);

            String currency = currentExpense.getCurrency() != null && !currentExpense.getCurrency().isEmpty()
                    ? currentExpense.getCurrency()
                    : "INR";
            String symbol = appConfig.getSymbol(currency);
            Label infoLabel = new Label(
                    split.getUser().getName() + " owes " + symbol
                            + split.getOwedAmount().stripTrailingZeros().toPlainString() + " " + currency);
            infoLabel.setPrefWidth(250);

            TextField paidField = new TextField(split.getPaidAmount().stripTrailingZeros().toPlainString());
            paidField.setPrefWidth(100);
            paidField.setPromptText("Paid");

            CheckBox cb = new CheckBox("Settled");
            cb.setSelected(split.isPaid());

            // Disable for those who paid enough to cover their share
            boolean isPayer = currentExpense.getPayments().stream()
                    .anyMatch(p -> p.getUser().getId().equals(split.getUser().getId()));

            if (isPayer && split.isPaid()) {
                infoLabel.setText(infoLabel.getText() + " (Payer)");
                paidField.setDisable(true);
                cb.setDisable(true);
                cb.setSelected(true);
            } else {
                paidField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) { // Focus lost
                        try {
                            BigDecimal amount = new BigDecimal(paidField.getText());
                            expenseService.updateSplitPayment(currentExpense.getId(), split.getId(), amount);
                            cb.setSelected(amount.compareTo(split.getOwedAmount()) >= 0);
                            changed = true;
                        } catch (NumberFormatException e) {
                            paidField.setText(split.getPaidAmount().toString());
                        }
                    }
                });

                cb.setOnAction(event -> {
                    expenseService.markSplitAsPaid(currentExpense.getId(), split.getId(), cb.isSelected());
                    paidField.setText(
                            cb.isSelected() ? split.getOwedAmount().stripTrailingZeros().toPlainString() : "0");
                    changed = true;
                });
            }

            row.getChildren().addAll(infoLabel, new Label("Paid:"), paidField, cb);
            splitsContainer.getChildren().add(row);
        }
    }

    public boolean isChanged() {
        return changed;
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
