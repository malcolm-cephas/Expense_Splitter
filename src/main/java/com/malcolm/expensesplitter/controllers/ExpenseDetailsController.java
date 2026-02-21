package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.services.ExpenseService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

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
            CheckBox cb = new CheckBox(
                    split.getUser().getName() + " owes " + appConfig.getCurrencySymbol() + split.getOwedAmount());
            cb.setSelected(split.isPaid());

            // Allow members to mark themselves as paid, EXCEPT the original payer (who paid
            // the full thing)
            if (split.getUser().getId().equals(currentExpense.getPaidBy().getId())) {
                cb.setText(cb.getText() + " (Payer)");
                cb.setDisable(true);
                cb.setSelected(true);
            } else {
                cb.setOnAction(event -> {
                    expenseService.markSplitAsPaid(currentExpense.getId(), split.getId(), cb.isSelected());
                    changed = true;
                });
            }

            splitsContainer.getChildren().add(cb);
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
