package com.malcolm.expensesplitter.controllers;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.dto.TransactionDto;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.UserRepository;
import com.malcolm.expensesplitter.services.SettlementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class SettleUpController {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppConfig appConfig;

    @FXML
    private ListView<TransactionDto> suggestionsListView;

    private Stage dialogStage;
    private Group currentGroup;

    @FXML
    public void initialize() {
        suggestionsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(TransactionDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    User from = userRepository.findById(item.getFrom()).orElse(null);
                    User to = userRepository.findById(item.getTo()).orElse(null);
                    String fromName = from != null ? from.getName() : "Unknown";
                    String toName = to != null ? to.getName() : "Unknown";

                    setText(fromName + " needs to pay " + toName + " : " + appConfig.getCurrencySymbol()
                            + item.getAmount());
                }
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        loadSuggestions();
    }

    private void loadSuggestions() {
        List<TransactionDto> transactions = settlementService.calculateSimplifiedDebts(currentGroup.getId());
        if (transactions.isEmpty()) {
            suggestionsListView.setPlaceholder(new Label("All settled up! No debts."));
        } else {
            suggestionsListView.setItems(FXCollections.observableArrayList(transactions));
        }
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
