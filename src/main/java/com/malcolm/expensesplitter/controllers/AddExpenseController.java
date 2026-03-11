package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.services.ExpenseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import com.malcolm.expensesplitter.config.AppConfig;

import java.math.BigDecimal;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.ExpenseSplit;
import com.malcolm.expensesplitter.models.SplitType;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;
import javafx.util.StringConverter;

@Controller
@Scope("prototype")
public class AddExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AppConfig appConfig;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<User> payerComboBox;

    @FXML
    private ComboBox<String> currencyComboBox;

    @FXML
    private javafx.scene.layout.VBox multiplePayersContainer;

    private User multiplePayersSentinel;

    @FXML
    private ComboBox<String> paymentModeComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private DatePicker expenseDatePicker;

    @FXML
    private TabPane splitTypeTabPane;

    @FXML
    private javafx.scene.layout.VBox membersCheckboxContainer;

    @FXML
    private javafx.scene.layout.VBox exactAmountContainer;

    @FXML
    private javafx.scene.layout.VBox percentageContainer;

    @FXML
    private javafx.scene.layout.VBox sharesContainer;

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

        payerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handlePayerSelection(newVal);
            }
        });

        currencyComboBox.setItems(
                FXCollections.observableArrayList("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED"));
        if (appConfig != null && appConfig.getCurrencyCode() != null) {
            currencyComboBox.getSelectionModel().select(appConfig.getCurrencyCode());
            multiplePayersSentinel = new User("Multiple Payers...", "multp@splitter.internal",
                    appConfig.getCurrencyCode());
        } else {
            currencyComboBox.getSelectionModel().selectFirst();
            multiplePayersSentinel = new User("Multiple Payers...", "multp@splitter.internal", "INR");
        }

        paymentModeComboBox
                .setItems(FXCollections.observableArrayList("Cash", "UPI", "Card", "Bank Transfer", "Other"));
        paymentModeComboBox.getSelectionModel().selectFirst();

        categoryComboBox.setItems(
                FXCollections.observableArrayList("Food & Drinks", "Travel", "Entertainment", "Rent", "Groceries",
                        "Utilities", "Other"));
        categoryComboBox.getSelectionModel().selectFirst();

        setupDatePicker();
    }

    private void setupDatePicker() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        expenseDatePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return formatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, formatter);
                } else {
                    return null;
                }
            }
        });
        expenseDatePicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        ObservableList<User> items = FXCollections.observableArrayList(group.getMembers());
        items.add(multiplePayersSentinel);
        payerComboBox.setItems(items);
        if (!group.getMembers().isEmpty()) {
            payerComboBox.getSelectionModel().selectFirst();
        }

        if (membersCheckboxContainer != null) {
            membersCheckboxContainer.getChildren().clear();
            exactAmountContainer.getChildren().clear();
            percentageContainer.getChildren().clear();
            sharesContainer.getChildren().clear();
            multiplePayersContainer.getChildren().clear();

            for (User member : group.getMembers()) {
                // Payments UI
                multiplePayersContainer.getChildren().add(createInputRow(member, "0"));
                // Equal Tab
                CheckBox cb = new CheckBox(member.getName());
                cb.setUserData(member.getId());
                cb.setSelected(true);
                membersCheckboxContainer.getChildren().add(cb);

                // Exact Tab
                exactAmountContainer.getChildren().add(createInputRow(member, "0"));
                // Percentage Tab
                percentageContainer.getChildren().add(createInputRow(member, "0"));
                // Shares Tab
                sharesContainer.getChildren().add(createInputRow(member, "1"));
            }
        }
    }

    private javafx.scene.layout.HBox createInputRow(User user, String defaultValue) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
        Label label = new Label(user.getName());
        label.setPrefWidth(150);
        TextField field = new TextField(defaultValue);
        field.setUserData(user.getId());
        field.setPrefWidth(100);
        row.getChildren().addAll(label, field);
        return row;
    }

    public void setExpenseToEdit(Expense expense) {
        this.expenseToEdit = expense;
        this.saveClicked = false; // Reset flag for new use

        if (expense != null) {
            descriptionField.setText(expense.getDescription());
            amountField.setText(expense.getAmount().stripTrailingZeros().toPlainString());
            payerComboBox.setValue(expense.getPaidBy());
            paymentModeComboBox.setValue(expense.getPaymentMode());
            categoryComboBox.setValue(expense.getCategory());
            expenseDatePicker.setValue(expense.getExpenseDate());
            if (expense.getCurrency() != null && !expense.getCurrency().isEmpty()) {
                currencyComboBox.setValue(expense.getCurrency());
            } else {
                currencyComboBox.getSelectionModel().selectFirst();
            }
            if (saveAndNewButton != null) {
                saveAndNewButton.setVisible(false);
                saveAndNewButton.setManaged(false);
            }

            // Multiple Payers UI
            if (expense.getPayments().size() > 1) {
                payerComboBox.getSelectionModel().select(multiplePayersSentinel);
                populatePaymentContainer(multiplePayersContainer, expense);
            } else {
                payerComboBox.getSelectionModel().select(expense.getPaidBy());
            }

            // Select the correct tab and populate values
            if (expense.getSplitType() == SplitType.EQUAL) {
                splitTypeTabPane.getSelectionModel().select(0);
                if (membersCheckboxContainer != null) {
                    Set<UUID> involvedIds = new HashSet<>();
                    for (ExpenseSplit split : expense.getSplits()) {
                        involvedIds.add(split.getUser().getId());
                    }
                    for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                        if (node instanceof CheckBox cb) {
                            cb.setSelected(involvedIds.contains((UUID) cb.getUserData()));
                        }
                    }
                }
            } else if (expense.getSplitType() == SplitType.EXACT) {
                splitTypeTabPane.getSelectionModel().select(1);
                populateInputContainer(exactAmountContainer, expense);
            } else if (expense.getSplitType() == SplitType.PERCENTAGE) {
                splitTypeTabPane.getSelectionModel().select(2);
                populateInputContainer(percentageContainer, expense);
            } else if (expense.getSplitType() == SplitType.SHARES) {
                splitTypeTabPane.getSelectionModel().select(3);
                populateInputContainer(sharesContainer, expense);
            }
        } else {
            // Reset for fresh ADD mode
            if (splitTypeTabPane != null)
                splitTypeTabPane.getSelectionModel().select(0);
            descriptionField.clear();
            amountField.clear();
            if (payerComboBox.getItems() != null && !payerComboBox.getItems().isEmpty()) {
                payerComboBox.getSelectionModel().selectFirst();
            }
            paymentModeComboBox.getSelectionModel().selectFirst();
            currencyComboBox.getSelectionModel().selectFirst();
            expenseDatePicker.setValue(LocalDate.now());

            if (saveAndNewButton != null) {
                saveAndNewButton.setVisible(true);
                saveAndNewButton.setManaged(true);
            }

            // Re-select all members by default
            if (membersCheckboxContainer != null) {
                for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                    if (node instanceof CheckBox cb)
                        cb.setSelected(true);
                }
            }
            multiplePayersContainer.getChildren().clear();
        }
    }

    private void handlePayerSelection(User selectedPayer) {
        boolean isMultiple = selectedPayer.equals(multiplePayersSentinel);
        multiplePayersContainer.setVisible(isMultiple);
        multiplePayersContainer.setManaged(isMultiple);
    }

    private void populatePaymentContainer(javafx.scene.layout.VBox container, Expense expense) {
        Map<UUID, BigDecimal> currentPayments = new HashMap<>();
        for (com.malcolm.expensesplitter.models.ExpensePayment payment : expense.getPayments()) {
            currentPayments.put(payment.getUser().getId(), payment.getAmount());
        }

        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof javafx.scene.layout.HBox row) {
                TextField tf = (TextField) row.getChildren().get(1);
                UUID userId = (UUID) tf.getUserData();
                if (currentPayments.containsKey(userId)) {
                    tf.setText(currentPayments.get(userId).stripTrailingZeros().toPlainString());
                } else {
                    tf.setText("0");
                }
            }
        }
    }

    private void populateInputContainer(javafx.scene.layout.VBox container, Expense expense) {
        Map<UUID, BigDecimal> currentSplits = new HashMap<>();
        for (ExpenseSplit split : expense.getSplits()) {
            currentSplits.put(split.getUser().getId(), split.getOwedAmount());
        }

        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof javafx.scene.layout.HBox row) {
                TextField tf = (TextField) row.getChildren().get(1);
                UUID userId = (UUID) tf.getUserData();
                if (currentSplits.containsKey(userId)) {
                    tf.setText(currentSplits.get(userId).stripTrailingZeros().toPlainString());
                } else {
                    tf.setText("0");
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
            BigDecimal amount = new BigDecimal(amountField.getText());
            String description = descriptionField.getText();

            String paymentMode = paymentModeComboBox.getValue();
            String category = categoryComboBox.getValue();
            SplitType splitType = SplitType.EQUAL;
            Map<UUID, BigDecimal> splitInputs = new java.util.HashMap<>();

            Tab selectedTab = splitTypeTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab.getText().contains("Equal")) {
                splitType = SplitType.EQUAL;
                for (javafx.scene.Node node : membersCheckboxContainer.getChildren()) {
                    if (node instanceof CheckBox cb && cb.isSelected()) {
                        splitInputs.put((UUID) cb.getUserData(), BigDecimal.ZERO);
                    }
                }
            } else if (selectedTab.getText().contains("Exact")) {
                splitType = SplitType.EXACT;
                for (javafx.scene.Node node : exactAmountContainer.getChildren()) {
                    if (node instanceof javafx.scene.layout.HBox row) {
                        TextField tf = (TextField) row.getChildren().get(1);
                        BigDecimal val = new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText());
                        splitInputs.put((UUID) tf.getUserData(), val);
                    }
                }
            } else if (selectedTab.getText().contains("Percentages")) {
                splitType = SplitType.PERCENTAGE;
                for (javafx.scene.Node node : percentageContainer.getChildren()) {
                    if (node instanceof javafx.scene.layout.HBox row) {
                        TextField tf = (TextField) row.getChildren().get(1);
                        BigDecimal val = new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText());
                        splitInputs.put((UUID) tf.getUserData(), val);
                    }
                }
            } else {
                splitType = SplitType.SHARES;
                for (javafx.scene.Node node : sharesContainer.getChildren()) {
                    if (node instanceof javafx.scene.layout.HBox row) {
                        TextField tf = (TextField) row.getChildren().get(1);
                        BigDecimal val = new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText());
                        splitInputs.put((UUID) tf.getUserData(), val);
                    }
                }
            }

            Map<UUID, BigDecimal> paymentInputs = new HashMap<>();
            if (payerComboBox.getSelectionModel().getSelectedItem().equals(multiplePayersSentinel)) {
                for (javafx.scene.Node node : multiplePayersContainer.getChildren()) {
                    if (node instanceof javafx.scene.layout.HBox row) {
                        TextField tf = (TextField) row.getChildren().get(1);
                        BigDecimal val = new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText());
                        if (val.compareTo(BigDecimal.ZERO) > 0) {
                            paymentInputs.put((UUID) tf.getUserData(), val);
                        }
                    }
                }
            } else {
                paymentInputs.put(payerComboBox.getValue().getId(), amount);
            }

            LocalDate expenseDate = expenseDatePicker.getValue();
            String currency = currencyComboBox.getValue();

            if (expenseToEdit != null) {
                expenseService.updateExpense(expenseToEdit.getId(), currentGroup.getId(), paymentInputs, amount,
                        description, paymentMode, category, expenseDate, splitType, splitInputs, currency);
            } else {
                expenseService.addExpense(currentGroup.getId(), paymentInputs, amount, description, paymentMode,
                        category, expenseDate, splitType, splitInputs, currency);
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
        if (expenseDatePicker.getValue() == null) {
            errorMessage += "No valid date selected!\n";
        }
        if (payerComboBox.getValue() == null) {
            errorMessage += "No valid payer selected!\n";
        }

        if (multiplePayersSentinel.equals(payerComboBox.getValue())) {
            BigDecimal totalPaid = BigDecimal.ZERO;
            for (javafx.scene.Node node : multiplePayersContainer.getChildren()) {
                if (node instanceof javafx.scene.layout.HBox row) {
                    TextField tf = (TextField) row.getChildren().get(1);
                    try {
                        totalPaid = totalPaid.add(new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText()));
                    } catch (NumberFormatException e) {
                        errorMessage += "Invalid payment amount for one of the members.\n";
                        break;
                    }
                }
            }
            try {
                BigDecimal expected = new BigDecimal(amountField.getText());
                if (totalPaid.compareTo(expected) != 0) {
                    errorMessage += "Total paid amounts (" + totalPaid + ") must equal expense amount (" + expected
                            + ")!\n";
                }
            } catch (Exception e) {
            }
        }

        // Tab specific validation
        Tab selectedTab = splitTypeTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab.getText().contains("Exact")) {
            BigDecimal total = BigDecimal.ZERO;
            for (javafx.scene.Node node : exactAmountContainer.getChildren()) {
                if (node instanceof javafx.scene.layout.HBox row) {
                    TextField tf = (TextField) row.getChildren().get(1);
                    try {
                        total = total.add(new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText()));
                    } catch (NumberFormatException e) {
                        errorMessage += "Invalid amount for one of the members.\n";
                        break;
                    }
                }
            }
            BigDecimal expected = new BigDecimal(amountField.getText());
            if (total.compareTo(expected) != 0) {
                errorMessage += "Total exact amounts (" + total + ") must equal expense amount (" + expected + ")!\n";
            }
        } else if (selectedTab.getText().contains("Percentages")) {
            BigDecimal totalPercent = BigDecimal.ZERO;
            for (javafx.scene.Node node : percentageContainer.getChildren()) {
                if (node instanceof javafx.scene.layout.HBox row) {
                    TextField tf = (TextField) row.getChildren().get(1);
                    try {
                        totalPercent = totalPercent.add(new BigDecimal(tf.getText().isEmpty() ? "0" : tf.getText()));
                    } catch (NumberFormatException e) {
                        errorMessage += "Invalid percentage for one of the members.\n";
                        break;
                    }
                }
            }
            if (totalPercent.compareTo(new BigDecimal("100")) != 0) {
                errorMessage += "Total percentage must be exactly 100% (currently " + totalPercent + "%)!\n";
            }
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
