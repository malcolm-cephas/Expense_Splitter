package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.services.ExpenseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Controller
@Scope("prototype")
public class AddExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<User> payerComboBox;

    @FXML
    private ComboBox<String> paymentModeComboBox;

    @FXML
    private TabPane splitTypeTabPane;

    @FXML
    private javafx.scene.layout.VBox membersCheckboxContainer;

    @FXML
    private Button saveAndNewButton;

    private Group currentGroup;
    private Stage dialogStage;
    private boolean saveClicked = false;
    private Expense expenseToEdit;

    @FXML
    public void initialize() {
        payerComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getEmail() + ")");
                }
            }
        });

        payerComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        paymentModeComboBox
                .setItems(FXCollections.observableArrayList("Cash", "UPI", "Card", "Bank Transfer", "Other"));
        paymentModeComboBox.getSelectionModel().selectFirst();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        payerComboBox.setItems(FXCollections.observableArrayList(group.getMembers()));
        if (!group.getMembers().isEmpty()) {
            payerComboBox.getSelectionModel().selectFirst();
        }

        if (membersCheckboxContainer != null) {
            membersCheckboxContainer.getChildren().clear();
            for (User member : group.getMembers()) {
                CheckBox cb = new CheckBox(member.getName() + " (" + member.getEmail() + ")");
                cb.setUserData(member.getId());
                cb.setSelected(true); // default to all selected
                membersCheckboxContainer.getChildren().add(cb);
            }
        }
    }

    public void setExpenseToEdit(Expense expense) {
        this.expenseToEdit = expense;
        this.saveClicked = false; // Reset flag for new use

        if (expense != null) {
            descriptionField.setText(expense.getDescription());
            amountField.setText(expense.getAmount().toString());
            payerComboBox.setValue(expense.getPaidBy());
            paymentModeComboBox.setValue(expense.getPaymentMode());
            if (saveAndNewButton != null) {
                saveAndNewButton.setVisible(false);
                saveAndNewButton.setManaged(false);
            }

            // Pre-check involved members
            if (membersCheckboxContainer != null) {
                Set<UUID> involvedIds = new HashSet<>();
                for (ExpenseSplit split : expense.getSplits()) {
                    involvedIds.add(split.getUser().getId());
                }
                for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                    if (node instanceof CheckBox cb) {
                        UUID memberId = (UUID) cb.getUserData();
                        cb.setSelected(involvedIds.contains(memberId));
                    }
                }
            }
        } else {
            // Reset fields for fresh ADD mode
            descriptionField.clear();
            amountField.clear();
            if (payerComboBox.getItems() != null && !payerComboBox.getItems().isEmpty()) {
                payerComboBox.getSelectionModel().selectFirst();
            }
            paymentModeComboBox.getSelectionModel().selectFirst();

            if (saveAndNewButton != null) {
                saveAndNewButton.setVisible(true);
                saveAndNewButton.setManaged(true);
            }

            // Re-select all members by default
            if (membersCheckboxContainer != null) {
                for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                    if (node instanceof CheckBox cb) {
                        cb.setSelected(true);
                    }
                }
            }
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (saveInternal()) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleSaveAndNew() {
        if (saveInternal()) {
            // Reset for next entry
            setExpenseToEdit(null);
            descriptionField.requestFocus();
        }
    }

    private boolean saveInternal() {
        if (isInputValid()) {
            User paidBy = payerComboBox.getValue();
            BigDecimal amount = new BigDecimal(amountField.getText());
            String description = descriptionField.getText();

            // Collect selected members
            Set<UUID> involvedMembers = new HashSet<>();
            if (membersCheckboxContainer != null) {
                for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                    if (node instanceof CheckBox cb && cb.isSelected()) {
                        involvedMembers.add((UUID) cb.getUserData());
                    }
                }
            }

            String paymentMode = paymentModeComboBox.getValue();

            if (expenseToEdit != null) {
                expenseService.updateEqualExpense(expenseToEdit.getId(), currentGroup.getId(), paidBy.getId(), amount,
                        description, paymentMode,
                        involvedMembers);
            } else {
                expenseService.addEqualExpense(currentGroup.getId(), paidBy.getId(), amount, description, paymentMode,
                        involvedMembers);
            }

            saveClicked = true;
            return true;
        }
        return false;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (descriptionField.getText() == null || descriptionField.getText().length() == 0) {
            errorMessage += "No valid description!\n";
        }
        if (amountField.getText() == null || amountField.getText().length() == 0) {
            errorMessage += "No valid amount!\n";
        } else {
            try {
                new BigDecimal(amountField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "No valid amount (must be a number)!\n";
            }
        }
        if (payerComboBox.getValue() == null) {
            errorMessage += "No valid payer selected!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
