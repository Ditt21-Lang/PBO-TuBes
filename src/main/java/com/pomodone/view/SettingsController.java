package com.pomodone.view;

import com.pomodone.model.user.User;
import com.pomodone.service.UserSettingsService;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;

public class SettingsController {

    private static final String FIELD_ERROR_STYLE = "field-error";
    private static final String ERROR_TITLE = "Error";

    // FXML Elements
    @FXML private TextField nameField;
    @FXML private Button updateNameButton;
    @FXML private Label nameErrorLabel;
    @FXML private TextField dailyTargetField;
    @FXML private Label dailyTargetErrorLabel;
    @FXML private TextField weeklyTargetField;
    @FXML private Label weeklyTargetErrorLabel;
    @FXML private Button resetTargetsButton;
    @FXML private Button saveTargetsButton;

    private UserSettingsService userSettingsService;
    
    // buat validasi
    private final BooleanProperty isNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isDailyTargetValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isWeeklyTargetValid = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        this.userSettingsService = new UserSettingsService();
        setupValidationListeners();
        loadUserSettings();

        updateNameButton.setOnAction(event -> handleUpdateName());
        saveTargetsButton.setOnAction(event -> handleSaveTargets());
        resetTargetsButton.setOnAction(event -> handleResetTargets());

        // matiin tombolnya kalo ga valid
        updateNameButton.disableProperty().bind(isNameValid.not());
        
        BooleanBinding areTargetsInvalid = isDailyTargetValid.not().or(isWeeklyTargetValid.not());
        saveTargetsButton.disableProperty().bind(areTargetsInvalid);
    }

    private void setupValidationListeners() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean isValid = newVal != null && !newVal.trim().isEmpty();
            isNameValid.set(isValid);
            toggleError(nameField, nameErrorLabel, !isValid, "Name cannot be empty.");
        });

        dailyTargetField.textProperty().addListener((obs, oldVal, newVal) -> 
            isDailyTargetValid.set(validatePositiveInteger(newVal, dailyTargetField, dailyTargetErrorLabel))
        );

        weeklyTargetField.textProperty().addListener((obs, oldVal, newVal) -> 
            isWeeklyTargetValid.set(validatePositiveInteger(newVal, weeklyTargetField, weeklyTargetErrorLabel))
        );
    }

    private boolean validatePositiveInteger(String value, TextField field, Label errorLabel) {
        if (value == null || value.trim().isEmpty()) {
            toggleError(field, errorLabel, true, "Cannot be empty.");
            return false;
        }
        if (value.contains(".") || value.contains(",")) {
            toggleError(field, errorLabel, true, "Decimals are not allowed.");
            return false;
        }
        try {
            int intValue = Integer.parseInt(value);
            if (intValue < 0) {
                toggleError(field, errorLabel, true, "Must be a positive number.");
                return false;
            }
        } catch (NumberFormatException e) {
            toggleError(field, errorLabel, true, "Must be a whole number.");
            return false;
        }
        toggleError(field, errorLabel, false, "");
        return true;
    }

    private void toggleError(Node field, Label errorLabel, boolean showError, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(showError);
        errorLabel.setManaged(showError);
        if (showError) {
            if (!field.getStyleClass().contains(FIELD_ERROR_STYLE)) {
                field.getStyleClass().add(FIELD_ERROR_STYLE);
            }
        } else {
            field.getStyleClass().remove(FIELD_ERROR_STYLE);
        }
    }

    private void loadUserSettings() {
        Optional<User> userOptional = userSettingsService.getCurrentUserSettings();
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            nameField.setText(user.getName());
            dailyTargetField.setText(String.valueOf(user.getDailyPomodoroTarget()));
            weeklyTargetField.setText(String.valueOf(user.getWeeklyPomodoroTarget()));
        } else {
            showAlert(Alert.AlertType.ERROR, ERROR_TITLE, "Failed to load user settings.");
            nameField.setDisable(true);
            dailyTargetField.setDisable(true);
            weeklyTargetField.setDisable(true);
        }
    }

    private void handleUpdateName() {
        boolean success = userSettingsService.updateUserName(nameField.getText());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Nama berhasil diupdate.");
        } else {
            showAlert(Alert.AlertType.ERROR, ERROR_TITLE, "Failed to update name.");
        }
    }

    private void handleSaveTargets() {
        try {
            int dailyTarget = Integer.parseInt(dailyTargetField.getText());
            int weeklyTarget = Integer.parseInt(weeklyTargetField.getText());

            boolean success = userSettingsService.updateUserTargets(dailyTarget, weeklyTarget);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Target berhasil disimpan.");
            } else {
                showAlert(Alert.AlertType.ERROR, ERROR_TITLE, "Failed to save targets.");
            }
        } catch (NumberFormatException e) {
            // harusnya gabisa nyampe sini kalo tombolnya udah di-disable
            showAlert(Alert.AlertType.ERROR, ERROR_TITLE, "Invalid number format.");
        }
    }

    private void handleResetTargets() {
        dailyTargetField.setText("5");
        weeklyTargetField.setText("25");
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
