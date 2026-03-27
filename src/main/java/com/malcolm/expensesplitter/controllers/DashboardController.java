package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.services.GroupService;
import com.malcolm.expensesplitter.services.ImportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;
import com.malcolm.expensesplitter.repositories.GroupRepository;
import com.malcolm.expensesplitter.repositories.ExpenseSplitRepository;
import com.malcolm.expensesplitter.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

import javafx.scene.control.TextInputDialog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for the Main Dashboard.
 * Handles group creation, selection, and switching between different group
 * views.
 */
@Controller
public class DashboardController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ImportService importService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @FXML
    private ListView<Group> groupListView;

    @FXML
    private TextField newGroupNameField;

    @FXML
    private TextField newGroupBudgetField;

    @FXML
    private javafx.scene.control.ToggleButton themeToggleButton;

    @FXML
    private VBox mainContentArea;

    @FXML
    private TextField currencySearchField;

    private ListView<String> currencySearchResults = new ListView<>();
    private javafx.stage.Popup currencyPopup = new javafx.stage.Popup();

    private ObservableList<Group> groupsObservable = FXCollections.observableArrayList();
    private ObservableList<String> allCurrencies = FXCollections.observableArrayList();
    private ObservableList<String> filteredCurrencies = FXCollections.observableArrayList();

    // Default user ID for testing since we don't have auth yet
    private UUID currentUserId;

    @FXML
    public void initialize() {
        // Cleanup legacy test user if it still exists
        try {
            userRepository.findByEmail("test@example.com").ifPresent(testUser -> {
                // 1. Delete all splits where this user is involved
                expenseSplitRepository.deleteAll(expenseSplitRepository.findByUserId(testUser.getId()));

                // 2. Clear user from all groups (membership and creation)
                for (Group group : groupRepository.findAll()) {
                    boolean modified = false;
                    if (group.getMembers().remove(testUser)) {
                        modified = true;
                    }
                    if (group.getCreatedBy() != null && group.getCreatedBy().equals(testUser)) {
                        group.setCreatedBy(null);
                        modified = true;
                    }
                    if (modified) {
                        groupRepository.save(group);
                    }
                }

                // 3. Delete expenses where they were the payer
                List<com.malcolm.expensesplitter.models.Expense> userPaidExpenses = expenseRepository.findAll().stream()
                        .filter(e -> e.getPaidBy() != null && e.getPaidBy().equals(testUser))
                        .toList();
                expenseRepository.deleteAll(userPaidExpenses);

                userRepository.delete(testUser);
                userRepository.flush();
            });
        } catch (Exception e) {
            System.err.println("Note: Cleanup of test user skipped or failed: " + e.getMessage());
        }

        // Fetch first user or prompt to create one
        Optional<com.malcolm.expensesplitter.models.User> firstUser = userRepository.findAll().stream().findFirst();
        if (firstUser.isPresent()) {
            com.malcolm.expensesplitter.models.User user = firstUser.get();
            currentUserId = user.getId();

            // Migrate user preference if null
            if (user.getCurrencyPreference() == null || user.getCurrencyPreference().isEmpty()) {
                user.setCurrencyPreference("INR");
                userRepository.save(user);
            }

            appConfig.setCurrencyCode(user.getCurrencyPreference());

            // One-time migration for existing expenses without currency
            List<com.malcolm.expensesplitter.models.Expense> expenses = expenseRepository.findAll();
            boolean migrated = false;
            for (com.malcolm.expensesplitter.models.Expense e : expenses) {
                if (e.getCurrency() == null || e.getCurrency().isEmpty()) {
                    e.setCurrency("INR");
                    migrated = true;
                }
            }
            if (migrated) {
                expenseRepository.saveAll(expenses);
            }
        } else {
            // No users exist, prompt for name and currency selection
            javafx.application.Platform.runLater(() -> {
                TextInputDialog nameDialog = new TextInputDialog();
                nameDialog.setTitle("Welcome to Expense Splitter");
                nameDialog.setHeaderText("Create your user profile");
                nameDialog.setContentText("Please enter your name:");

                Optional<String> nameResult = nameDialog.showAndWait();
                nameResult.ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        javafx.scene.control.ChoiceDialog<String> currencyDialog = new javafx.scene.control.ChoiceDialog<>(
                                "INR",
                                java.util.Arrays.asList("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED"));
                        currencyDialog.setTitle("Initial Setup");
                        currencyDialog.setHeaderText("Select your primary currency");
                        currencyDialog.setContentText("This will be your default for new expenses:");

                        Optional<String> currencyResult = currencyDialog.showAndWait();
                        String chosenCurrency = currencyResult.orElse("INR");

                        com.malcolm.expensesplitter.models.User newUser = new com.malcolm.expensesplitter.models.User(
                                name.trim(), name.trim().toLowerCase().replace(" ", "") + "@example.com",
                                chosenCurrency);
                        userRepository.save(newUser);
                        currentUserId = newUser.getId();
                        appConfig.setCurrencyCode(chosenCurrency);
                        // Optionally update appConfig if needed, but the app should ideally
                        // use the current user's preference for group views.
                    }
                });
            });
        }

        groupListView.setItems(groupsObservable);

        // Custom cell factory to show group name nicely
        groupListView.setCellFactory(param -> new javafx.scene.control.ListCell<Group>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getName() == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.getName());
                    
                    // Add context menu for CRUD
                    javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
                    javafx.scene.control.MenuItem renameItem = new javafx.scene.control.MenuItem("Rename Group");
                    renameItem.setOnAction(e -> handleRenameGroup(item));
                    
                    javafx.scene.control.MenuItem budgetItem = new javafx.scene.control.MenuItem("Set Group Budget");
                    budgetItem.setOnAction(e -> handleSetBudget(item));
                    
                    javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete Group");
                    deleteItem.setStyle("-fx-text-fill: red;");
                    deleteItem.setOnAction(e -> handleDeleteGroup(item));
                    
                    contextMenu.getItems().addAll(renameItem, budgetItem, deleteItem);
                    setContextMenu(contextMenu);
                }
            }
        });

        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadGroupView(newSelection.getId());
            }
        });

        // Load groups later when auth is injected, for now, we'll try to load all
        // Load groups later when auth is injected, for now, we'll try to load all
        refreshGroups();
        setupSearchableCurrency();
        
        // Initialize theme toggle state
        if (themeToggleButton != null) {
            boolean isDark = javafx.application.Application.getUserAgentStylesheet().contains("PrimerDark");
            themeToggleButton.setSelected(isDark);
            themeToggleButton.setText(isDark ? "🌙 Dark" : "☀️ Light");
        }
    }

    private void setupSearchableCurrency() {
        // Configure the ListView for the popup
        currencySearchResults.setPrefHeight(250.0);
        currencySearchResults.setPrefWidth(250.0);
        currencySearchResults.getStyleClass().add("popover");
        currencySearchResults.setFixedCellSize(35);
        currencySearchResults.setItems(filteredCurrencies);
        
        currencyPopup.getContent().add(currencySearchResults);
        currencyPopup.setAutoHide(true);

        // Build a mapping of Currency Code (Country)
        java.util.Locale[] locales = java.util.Locale.getAvailableLocales();
        java.util.Set<String> entries = new java.util.HashSet<>();
        for (java.util.Locale locale : locales) {
            try {
                java.util.Currency currency = java.util.Currency.getInstance(locale);
                String country = locale.getDisplayCountry();
                if (!country.isEmpty()) {
                    entries.add(currency.getCurrencyCode() + " (" + country + ")");
                }
            } catch (Exception e) {}
        }
        
        // Add common Crypto assets manually as java.util.Currency doesn't include them
        entries.add("BTC (Bitcoin)");
        entries.add("ETH (Ethereum)");
        entries.add("SOL (Solana)");
        entries.add("USDT (Tether)");
        entries.add("BNB (Binance)");
        entries.add("MATIC (Polygon)");
        entries.add("DOGE (Dogecoin)");
        entries.add("DOT (Polkadot)");
        entries.add("ADA (Cardano)");

        allCurrencies.setAll(entries);
        FXCollections.sort(allCurrencies);
        filteredCurrencies.setAll(allCurrencies);

        if (appConfig != null && appConfig.getCurrencyCode() != null) {
            currencySearchField.setText(appConfig.getCurrencyCode());
        }

        // Search logic
        currencySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String search = (newVal == null) ? "" : newVal.toUpperCase();
            List<String> matches = allCurrencies.stream()
                    .filter(c -> c.toUpperCase().contains(search))
                    .collect(java.util.stream.Collectors.toList());
            filteredCurrencies.setAll(matches);
            
            if (!matches.isEmpty() && currencySearchField.isFocused()) {
                showCurrencyPopup();
            } else {
                currencyPopup.hide();
            }
        });

        // Toggle on focus
        currencySearchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !filteredCurrencies.isEmpty()) {
                showCurrencyPopup();
            } else {
                // Focus lost: hide popup
                currencyPopup.hide();
            }
        });

        // Selection logic
        currencySearchResults.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = newVal.substring(0, 3);
                updateDefaultCurrency(code);
                currencySearchField.setText(code);
                currencyPopup.hide();
            }
        });
    }

    private void showCurrencyPopup() {
        if (!currencyPopup.isShowing() && currencySearchField.getScene() != null) {
            javafx.geometry.Point2D pos = currencySearchField.localToScreen(0, currencySearchField.getHeight());
            currencyPopup.show(currencySearchField, pos.getX(), pos.getY() + 5);
        }
    }

    private void updateDefaultCurrency(String currencyCode) {
        if (currentUserId != null) {
            com.malcolm.expensesplitter.models.User user = userRepository.findById(currentUserId).orElse(null);
            if (user != null) {
                user.setCurrencyPreference(currencyCode);
                userRepository.save(user);
                appConfig.setCurrencyCode(currencyCode);
                
                // Show a small confirmation toast or alert would be nice, but simple update is fine
                System.out.println("Default currency updated to: " + currencyCode);
            }
        }
    }

    @FXML
    public void onCreateGroup() {
        String name = newGroupNameField.getText().trim();
        String budgetText = newGroupBudgetField.getText().trim();
        java.math.BigDecimal budget = java.math.BigDecimal.ZERO;
        
        if (!budgetText.isEmpty()) {
            try {
                budget = new java.math.BigDecimal(budgetText);
            } catch (NumberFormatException e) {
                // Show error or just use zero
            }
        }

        if (!name.isEmpty() && currentUserId != null) {
            groupService.createGroup(name, "Group expenses and splitting", currentUserId, budget, appConfig.getCurrencyCode());
            newGroupNameField.clear();
            newGroupBudgetField.clear();
            refreshGroups();
        }
    }

    @FXML
    public void handleImportGroup() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Group Backup File");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Backup", "*.json"));
        
        java.io.File file = fileChooser.showOpenDialog(groupListView.getScene().getWindow());
        if (file != null) {
            javafx.concurrent.Task<Group> importTask = new javafx.concurrent.Task<>() {
                @Override
                protected Group call() throws Exception {
                    return importService.importGroupFromBackup(file);
                }
            };

            importTask.setOnSucceeded(event -> {
                refreshGroups();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText(null);
                alert.setContentText("Group has been imported successfully: " + importTask.getValue().getName());
                alert.showAndWait();
            });

            importTask.setOnFailed(event -> {
                importTask.getException().printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Import Failed");
                alert.setHeaderText("An error occurred during import");
                alert.setContentText(importTask.getException().getMessage());
                alert.showAndWait();
            });

            new Thread(importTask).start();
        }
    }

    private void refreshGroups() {
        groupsObservable.setAll(groupService.getAllGroups());
    }

    private void handleRenameGroup(Group group) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(group.getName());
        dialog.setTitle("Rename Group");
        dialog.setHeaderText("Change group name");
        dialog.setContentText("Enter new name:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                groupService.updateGroup(group.getId(), newName.trim());
                refreshGroups();
            }
        });
    }

    private void handleDeleteGroup(Group group) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Group");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete '" + group.getName() + "'? This will delete all associated expenses.");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            groupService.deleteGroup(group.getId());
            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(new javafx.scene.control.Label("Group deleted. Select another group."));
            refreshGroups();
        }
    }

    private void handleSetBudget(Group group) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
            group.getBudget() != null ? group.getBudget().toString() : "0");
        dialog.setTitle("Set Group Budget");
        dialog.setHeaderText("Specify budget for '" + group.getName() + "'");
        dialog.setContentText("Budget Amount:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(amountStr.trim());
                String currentCurrency = appConfig.getCurrencyCode();
                groupService.updateGroupBudget(group.getId(), amount, currentCurrency);
                
                // If this group is currently being viewed, reload it
                if (groupListView.getSelectionModel().getSelectedItem() != null && 
                    groupListView.getSelectionModel().getSelectedItem().getId().equals(group.getId())) {
                    loadGroupView(group.getId());
                }
                
                refreshGroups();
            } catch (NumberFormatException e) {
                javafx.scene.control.Alert error = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                error.setTitle("Invalid Input");
                error.setHeaderText(null);
                error.setContentText("Please enter a valid numeric value for the budget.");
                error.showAndWait();
            }
        });
    }

    private void loadGroupView(UUID groupId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent groupView = loader.load();

            GroupViewController controller = loader.getController();
            controller.setGroupId(groupId);

            mainContentArea.getChildren().clear();
            mainContentArea.getChildren().add(groupView);

            // Make groupView grow to fill space
            VBox.setVgrow(groupView, javafx.scene.layout.Priority.ALWAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleThemeToggle() {
        boolean isDark = themeToggleButton.isSelected();
        if (isDark) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            themeToggleButton.setText("🌙 Dark");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            themeToggleButton.setText("☀️ Light");
        }
    }

    // For injecting current user from main app
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
    }
}
