package com.pomodone.view;

import com.pomodone.model.user.User;
import com.pomodone.service.UserSettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.util.Optional;

public class SettingsController {

    @FXML
    private TextField nameField;

    @FXML
    private Button updateNameButton;

    @FXML
    private TextField dailyTargetField;

    @FXML
    private TextField weeklyTargetField;

    @FXML
    private Button resetTargetsButton;

    @FXML
    private Button saveTargetsButton;

    private UserSettingsService userSettingsService;

    @FXML
    public void initialize() {
        this.userSettingsService = new UserSettingsService();
        loadUserSettings();

        updateNameButton.setOnAction(event -> handleUpdateName());
        saveTargetsButton.setOnAction(event -> handleSaveTargets());
        resetTargetsButton.setOnAction(event -> handleResetTargets());
    }

    private void loadUserSettings() {
        Optional<User> userOptional = userSettingsService.getCurrentUserSettings();
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            nameField.setText(user.getName());
            dailyTargetField.setText(String.valueOf(user.getDailyPomodoroTarget()));
            weeklyTargetField.setText(String.valueOf(user.getWeeklyPomodoroTarget()));
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load user settings.");
            // Disable fields if user not found
            nameField.setDisable(true);
            dailyTargetField.setDisable(true);
            weeklyTargetField.setDisable(true);
        }
    }

    private void handleUpdateName() {
        String newName = nameField.getText();
        if (newName == null || newName.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name cannot be empty.");
            return;
        }

        boolean success = userSettingsService.updateUserName(newName);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Name updated successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update name.");
        }
    }

    private void handleSaveTargets() {
        try {
            int dailyTarget = Integer.parseInt(dailyTargetField.getText());
            int weeklyTarget = Integer.parseInt(weeklyTargetField.getText());

            if (dailyTarget < 0 || weeklyTarget < 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Targets cannot be negative.");
                return;
            }

            boolean success = userSettingsService.updateUserTargets(dailyTarget, weeklyTarget);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Targets saved successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save targets.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter valid numbers for targets.");
        }
    }

    private void handleResetTargets() {
        // Reset to some default values, e.g., 0 or load from a config
        dailyTargetField.setText("0");
        weeklyTargetField.setText("0");
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}