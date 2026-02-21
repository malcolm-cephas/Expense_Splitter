package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.services.GroupService;
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

import javafx.scene.control.TextInputDialog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class DashboardController {

    @Autowired
    private GroupService groupService;

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
    private VBox mainContentArea;

    private ObservableList<Group> groupsObservable = FXCollections.observableArrayList();

    // Default user ID for testing since we don't have auth yet
    private UUID currentUserId;

    @FXML
    public void initialize() {
        // Fetch first user or prompt to create one
        Optional<com.malcolm.expensesplitter.models.User> firstUser = userRepository.findAll().stream().findFirst();
        if (firstUser.isPresent()) {
            currentUserId = firstUser.get().getId();
        } else {
            // No users exist, prompt for name
            javafx.application.Platform.runLater(() -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Welcome to Expense Splitter");
                dialog.setHeaderText("Create your user profile");
                dialog.setContentText("Please enter your name:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        com.malcolm.expensesplitter.models.User newUser = new com.malcolm.expensesplitter.models.User(
                                name.trim(), name.trim().toLowerCase().replace(" ", "") + "@example.com",
                                appConfig.getCurrencyCode());
                        userRepository.save(newUser);
                        currentUserId = newUser.getId();
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
                } else {
                    setText(item.getName());
                }
            }
        });

        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadGroupView(newSelection.getId());
            }
        });

        // Load groups later when auth is injected, for now, we'll try to load all
        refreshGroups();
    }

    @FXML
    public void onCreateGroup() {
        String name = newGroupNameField.getText().trim();
        if (!name.isEmpty() && currentUserId != null) {
            groupService.createGroup(name, "New group created from UI", currentUserId);
            newGroupNameField.clear();
            refreshGroups();
        }
    }

    private void refreshGroups() {
        groupsObservable.setAll(groupService.getAllGroups());
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

    // For injecting current user from main app
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
    }
}
