package com.malcolm.expensesplitter.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Scope("prototype")
public class CurrencySelectionController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<CurrencyItem> currencyListView;

    private Stage dialogStage;
    private String selectedCurrencyCode;
    private boolean selectionConfirmed = false;

    private final ObservableList<CurrencyItem> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        FilteredList<CurrencyItem> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (item.code().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return item.name().toLowerCase().contains(lowerCaseFilter);
            });
        });

        currencyListView.setItems(filteredData);
        currencyListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CurrencyItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.code().toUpperCase() + " - " + item.name());
                }
            }
        });

        currencyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleConfirm();
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCurrencies(Map<String, String> currencies) {
        masterData.clear();
        masterData.addAll(currencies.entrySet().stream()
                .map(e -> {
                    String code = e.getKey().toUpperCase();
                    String name = e.getValue();
                    if (name == null || name.trim().isEmpty()) {
                        name = code; // Fallback to code if name is empty
                    }
                    return new CurrencyItem(code, name);
                })
                .sorted((a, b) -> a.code().compareToIgnoreCase(b.code()))
                .collect(Collectors.toList()));
    }

    public String getSelectedCurrencyCode() {
        return selectedCurrencyCode;
    }

    public boolean isSelectionConfirmed() {
        return selectionConfirmed;
    }

    @FXML
    private void handleConfirm() {
        CurrencyItem selected = currencyListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedCurrencyCode = selected.code().toUpperCase();
            selectionConfirmed = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    public record CurrencyItem(String code, String name) {}
}
