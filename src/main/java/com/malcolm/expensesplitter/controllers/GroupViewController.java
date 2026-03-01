package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.services.ExpenseService;
import com.malcolm.expensesplitter.services.GroupService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.services.ExportService;
import com.malcolm.expensesplitter.services.SettlementService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for the Group View in the JavaFX application.
 * Manages group details, member interactions, expense logging,
 * debt visualization, and data export.
 */
@Controller
public class GroupViewController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private ExportService exportService;

    @FXML
    private Label groupNameLabel;

    @FXML
    private Label groupDescriptionLabel;

    @FXML
    private ListView<Expense> expenseListView;

    @FXML
    private ListView<User> memberListView;

    private Group currentGroup;
    private ObservableList<Expense> expensesObservable = FXCollections.observableArrayList();
    private ObservableList<User> membersObservable = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        expenseListView.setItems(expensesObservable);
        memberListView.setItems(membersObservable);

        memberListView.setCellFactory(param -> new javafx.scene.control.ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.getName() + " (" + item.getEmail() + ")");
                    // Ensure the item is selected on right click if not already
                    setOnMousePressed(event -> {
                        if (event.isSecondaryButtonDown()) {
                            memberListView.getSelectionModel().select(item);
                        }
                    });
                }
            }
        });

        expenseListView.setCellFactory(param -> new javafx.scene.control.ListCell<Expense>() {
            @Override
            protected void updateItem(Expense item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    String mode = item.getPaymentMode() != null ? " via " + item.getPaymentMode() : "";
                    String category = item.getCategory() != null ? " [" + item.getCategory() + "]" : "";
                    setText(item.getDescription() + category + " - " + appConfig.getCurrencySymbol() + item.getAmount()
                            .setScale(0, java.math.RoundingMode.CEILING) + " "
                            + item.getCurrency() + mode);
                    // Ensure the item is selected on right click
                    setOnMousePressed(event -> {
                        if (event.isSecondaryButtonDown()) {
                            expenseListView.getSelectionModel().select(item);
                        }
                    });
                }
            }
        });

        expenseListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Expense selectedExpense = expenseListView.getSelectionModel().getSelectedItem();
                if (selectedExpense != null) {
                    showExpenseDetails(selectedExpense);
                }
            }
        });
    }

    public void setGroupId(UUID groupId) {
        this.currentGroup = groupService.getGroup(groupId);
        if (currentGroup != null) {
            groupNameLabel.setText(currentGroup.getName());
            String desc = currentGroup.getDescription() != null ? currentGroup.getDescription() : "";
            groupDescriptionLabel.setText(desc + " (" + currentGroup.getMembers().size() + " members)");

            expensesObservable.setAll(expenseService.getGroupExpenses(groupId));
            membersObservable.setAll(currentGroup.getMembers());
        }
    }

    private void refreshExpenses() {
        if (currentGroup != null) {
            expensesObservable.setAll(expenseService.getGroupExpenses(currentGroup.getId()));
        }
    }

    @FXML
    public void handleEditExpense() {
        Expense selected = expenseListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentGroup != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense_modal.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent page = loader.load();

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Edit Expense");
                dialogStage.initModality(Modality.WINDOW_MODAL);

                Scene scene = new Scene(page);
                scene.getStylesheets().addAll(groupNameLabel.getScene().getStylesheets());
                dialogStage.setScene(scene);

                AddExpenseController controller = loader.getController();
                controller.setDialogStage(dialogStage);
                controller.setGroup(currentGroup);
                controller.setExpenseToEdit(selected); // Custom method we added

                dialogStage.showAndWait();

                if (controller.isSaveClicked()) {
                    refreshExpenses();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleDeleteExpense() {
        // ...
        Expense selected = expenseListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            expenseService.deleteExpense(selected.getId());
            refreshExpenses();
        }
    }

    @FXML
    public void handleEditMember() {
        User selected = memberListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentGroup != null) {
            TextInputDialog dialog = new TextInputDialog(selected.getName());
            dialog.setTitle("Edit Member");
            dialog.setHeaderText("Edit member name");
            dialog.setContentText("Please enter new name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    groupService.updateMemberName(selected.getId(), name.trim());
                    currentGroup = groupService.getGroup(currentGroup.getId()); // reload
                    membersObservable.setAll(currentGroup.getMembers());
                }
            });
        }
    }

    @FXML
    public void handleRemoveMember() {
        User selected = memberListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentGroup != null) {
            currentGroup = groupService.removeMemberFromGroup(currentGroup.getId(), selected.getId());
            String desc = currentGroup.getDescription() != null ? currentGroup.getDescription() : "";
            groupDescriptionLabel.setText(desc + " (" + currentGroup.getMembers().size() + " members)");
            membersObservable.setAll(currentGroup.getMembers());
        }
    }

    @FXML
    public void handleAddExpense() {
        if (currentGroup == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense_modal.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Expense");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(groupNameLabel.getScene().getStylesheets());
            dialogStage.setScene(scene);

            AddExpenseController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setGroup(currentGroup);
            controller.setExpenseToEdit(null); // Ensure fresh state for adding

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                refreshExpenses();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddMember() {
        if (currentGroup == null)
            return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Add a new member to the group");
        dialog.setContentText("Please enter member name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                currentGroup = groupService.addMemberToGroupByName(currentGroup.getId(), name.trim());
                String desc = currentGroup.getDescription() != null ? currentGroup.getDescription() : "";
                groupDescriptionLabel.setText(desc + " (" + currentGroup.getMembers().size() + " members)");
                membersObservable.setAll(currentGroup.getMembers());
            }
        });
    }

    @FXML
    public void handleSettleUp() {
        if (currentGroup == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settle_up_modal.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settle Up Suggestions");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(groupNameLabel.getScene().getStylesheets());
            dialogStage.setScene(scene);

            SettleUpController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setGroup(currentGroup);

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showExpenseDetails(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expense_details_modal.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Expense Details");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(groupNameLabel.getScene().getStylesheets());
            dialogStage.setScene(scene);

            ExpenseDetailsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setExpense(expense);

            dialogStage.showAndWait();

            if (controller.isChanged()) {
                refreshExpenses();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExport() {
        if (currentGroup == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Group Summary");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(currentGroup.getName().replace(" ", "_") + "_Report");

        java.io.File file = fileChooser.showSaveDialog(groupNameLabel.getScene().getWindow());
        if (file != null) {
            try {
                List<Expense> expenses = expenseService.getGroupExpenses(currentGroup.getId());
                List<TransactionDto> settlements = settlementService.calculateSimplifiedDebts(currentGroup.getId());

                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    exportService.exportGroupToPdf(currentGroup, expenses, settlements, file);
                } else {
                    try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                        exportService.exportGroupToCsv(currentGroup, expenses, settlements, writer);
                    }
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Report exported to: " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("An error occurred during export");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    public void handleShowStats() {
        if (currentGroup == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_modal.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Spending Statistics");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(groupNameLabel.getScene().getStylesheets());
            dialogStage.setScene(scene);

            StatisticsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setData(expenseService.getGroupExpenses(currentGroup.getId()));

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
