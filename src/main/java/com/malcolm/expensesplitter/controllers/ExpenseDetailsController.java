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
import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class ExpenseDetailsController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AppConfig appConfig;

    @FXML
    private Label expenseInfoLabel;

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
        expenseInfoLabel.setText(
                expense.getDescription() + " - " + appConfig.getCurrencySymbol() + expense.getAmount() + " (Paid by "
                        + expense.getPaidBy().getName() + ")");

        loadSplits();
    }

    private void loadSplits() {
        splitsContainer.getChildren().clear();
        for (ExpenseSplit split : currentExpense.getSplits()) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);

            Label infoLabel = new Label(
                    split.getUser().getName() + " owes " + appConfig.getCurrencySymbol()
                            + split.getOwedAmount().setScale(0, RoundingMode.CEILING));
            infoLabel.setPrefWidth(250);

            TextField paidField = new TextField(split.getPaidAmount().setScale(0, RoundingMode.CEILING).toString());
            paidField.setPrefWidth(100);
            paidField.setPromptText("Paid");

            CheckBox cb = new CheckBox("Settled");
            cb.setSelected(split.isPaid());

            // Disable for original payer as they are already settled
            if (split.getUser().getId().equals(currentExpense.getPaidBy().getId())) {
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
                            cb.isSelected() ? split.getOwedAmount().setScale(0, RoundingMode.CEILING).toString() : "0");
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
