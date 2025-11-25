package com.pomodone.view;

import com.pomodone.model.pomodoro.PomodoroSettings;
import com.pomodone.service.PomodoroService;
import com.pomodone.strategy.pomodoro.ClassicPomodoroStrategy;
import com.pomodone.strategy.pomodoro.IntensePomodoroStrategy;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PomodoroController {

    //<editor-fold desc="FXML Elements">
    @FXML private Label statusLabel;
    @FXML private Label hoursLabel;
    @FXML private Label minutesLabel;
    @FXML private Label secondsLabel;
    @FXML private VBox hoursGroup;
    @FXML private Label separatorLabel1;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private ProgressBar progressBar;
    @FXML private HBox modeSelectionBox;
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private ToggleButton classicModeButton;
    @FXML private ToggleButton intenseModeButton;
    @FXML private ToggleButton customModeButton;
    @FXML private Label settingsTitleLabel;
    @FXML private GridPane displayGridPane;
    @FXML private GridPane inputFieldsGridPane;
    @FXML private Label displayFocusLabel;
    @FXML private Label displayShortBreakLabel;
    @FXML private Label displayLongBreakLabel;
    @FXML private Label displayRoundsLabel;
    @FXML private TextField customFocusField;
    @FXML private TextField customShortBreakField;
    @FXML private TextField customLongBreakField;
    @FXML private TextField customRoundsField;
    @FXML private Label focusErrorLabel;
    @FXML private Label shortBreakErrorLabel;
    @FXML private Label longBreakErrorLabel;
    @FXML private Label roundsErrorLabel;
    //</editor-fold>

    private PomodoroService pomodoroService;

    // Validation Properties
    private final BooleanProperty isFocusValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isShortBreakValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isLongBreakValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isRoundsValid = new SimpleBooleanProperty(true);

    @FXML
    public void initialize() {
        this.pomodoroService = PomodoroService.getInstance();
        bindUIToService();
        setupActionHandlers();
        setupValidationListeners();
        updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
    }

    private void bindUIToService() {
        // Bind labels and progress bar
        statusLabel.textProperty().bind(pomodoroService.statusStringProperty());
        hoursLabel.textProperty().bind(pomodoroService.hoursProperty());
        minutesLabel.textProperty().bind(pomodoroService.minutesProperty());
        secondsLabel.textProperty().bind(pomodoroService.secondsProperty());
        progressBar.progressProperty().bind(pomodoroService.progressProperty());
        
        // Bind visibility of hours
        hoursGroup.visibleProperty().bind(pomodoroService.showHoursProperty());
        hoursGroup.managedProperty().bind(pomodoroService.showHoursProperty());
        separatorLabel1.visibleProperty().bind(pomodoroService.showHoursProperty());
        separatorLabel1.managedProperty().bind(pomodoroService.showHoursProperty());

        pomodoroService.timerStateProperty().addListener((obs, oldState, newState) -> {
            boolean isStopped = newState == PomodoroService.TimerState.STOPPED;
            modeSelectionBox.setDisable(!isStopped);
            inputFieldsGridPane.setDisable(!isStopped);

            switch (newState) {
                case STOPPED: startButton.setText("Start"); break;
                case RUNNING: startButton.setText("Pause"); break;
                case PAUSED: startButton.setText("Resume"); break;
            }
        });
        
        // Listen to validity changes to enable/disable start button in custom mode
        isFocusValid.addListener((obs, o, n) -> updateStartButtonState());
        isShortBreakValid.addListener((obs, o, n) -> updateStartButtonState());
        isLongBreakValid.addListener((obs, o, n) -> updateStartButtonState());
        isRoundsValid.addListener((obs, o, n) -> updateStartButtonState());
        customModeButton.selectedProperty().addListener((obs, o, n) -> updateStartButtonState());
    }

    private void setupActionHandlers() {
        startButton.setOnAction(event -> pomodoroService.handleStartPause());
        stopButton.setOnAction(event -> pomodoroService.stopAndResetTimer());

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true); return;
            }
            if (newToggle == classicModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
                pomodoroService.selectMode(PomodoroService.PomodoroMode.CLASSIC);
            } else if (newToggle == intenseModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.INTENSE);
                pomodoroService.selectMode(PomodoroService.PomodoroMode.INTENSE);
            } else if (newToggle == customModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CUSTOM);
                validateAllCustomFields(); // Validate once on switch
                applyCustomSettings();
            }
        });
    }

    private void updateStartButtonState() {
        boolean isCustomMode = customModeButton.isSelected();
        boolean areCustomFieldsInvalid = !isFocusValid.get() || !isShortBreakValid.get() || !isLongBreakValid.get() || !isRoundsValid.get();
        
        if (isCustomMode && areCustomFieldsInvalid) {
            startButton.setDisable(true);
        } else {
            startButton.setDisable(false);
        }
    }

    private void setupValidationListeners() {
        customFocusField.textProperty().addListener((obs, oldV, newV) -> {
            if (isFocusValid.get() != validatePositiveInteger(newV, customFocusField, focusErrorLabel, false)) {
                isFocusValid.set(!isFocusValid.get());
            }
            if (isFocusValid.get()) applyCustomSettings();
        });
        customShortBreakField.textProperty().addListener((obs, oldV, newV) -> {
            if (isShortBreakValid.get() != validatePositiveInteger(newV, customShortBreakField, shortBreakErrorLabel, true)) {
                isShortBreakValid.set(!isShortBreakValid.get());
            }
            if(isShortBreakValid.get()) applyCustomSettings();
        });
        customLongBreakField.textProperty().addListener((obs, oldV, newV) -> {
            if (isLongBreakValid.get() != validatePositiveInteger(newV, customLongBreakField, longBreakErrorLabel, true)) {
                isLongBreakValid.set(!isLongBreakValid.get());
            }
            if(isLongBreakValid.get()) applyCustomSettings();
        });
        customRoundsField.textProperty().addListener((obs, oldV, newV) -> {
            if (isRoundsValid.get() != validatePositiveInteger(newV, customRoundsField, roundsErrorLabel, false)) {
                isRoundsValid.set(!isRoundsValid.get());
            }
            if(isRoundsValid.get()) applyCustomSettings();
        });
    }
    
    private boolean validatePositiveInteger(String value, TextField field, Label errorLabel, boolean allowZero) {
        if (value == null || value.trim().isEmpty()) {
            toggleError(field, errorLabel, true, "Cannot be empty.");
            return false;
        }
        if (value.contains(".") || value.contains(",")) {
            toggleError(field, errorLabel, true, "Decimals not allowed.");
            return false;
        }
        try {
            int intValue = Integer.parseInt(value);
            if (allowZero ? intValue < 0 : intValue <= 0) {
                toggleError(field, errorLabel, true, "Must be positive.");
                return false;
            }
        } catch (NumberFormatException e) {
            toggleError(field, errorLabel, true, "Must be a valid number.");
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
            if (!field.getStyleClass().contains("field-error")) field.getStyleClass().add("field-error");
        } else {
            field.getStyleClass().remove("field-error");
        }
    }
    
    private void validateAllCustomFields() {
        isFocusValid.set(validatePositiveInteger(customFocusField.getText(), customFocusField, focusErrorLabel, false));
        isShortBreakValid.set(validatePositiveInteger(customShortBreakField.getText(), customShortBreakField, shortBreakErrorLabel, true));
        isLongBreakValid.set(validatePositiveInteger(customLongBreakField.getText(), customLongBreakField, longBreakErrorLabel, true));
        isRoundsValid.set(validatePositiveInteger(customRoundsField.getText(), customRoundsField, roundsErrorLabel, false));
    }
    
    private void updateSettingsView(PomodoroService.PomodoroMode mode) {
        boolean isCustom = mode == PomodoroService.PomodoroMode.CUSTOM;
        displayGridPane.setVisible(!isCustom);
        displayGridPane.setManaged(!isCustom);
        inputFieldsGridPane.setVisible(isCustom);
        inputFieldsGridPane.setManaged(isCustom);
        
        PomodoroSettings settings;
        switch (mode) {
            case CLASSIC:
                settings = new ClassicPomodoroStrategy().getSettings();
                settingsTitleLabel.setText("Classic Settings");
                populateDisplayLabels(settings);
                break;
            case INTENSE:
                settings = new IntensePomodoroStrategy().getSettings();
                settingsTitleLabel.setText("Intense Settings");
                populateDisplayLabels(settings);
                break;
            default:
                settingsTitleLabel.setText("Custom Settings");
                break;
        }
    }
    
    private void populateDisplayLabels(PomodoroSettings settings) {
        displayFocusLabel.setText(settings.getFocusDuration().toMinutes() + " minutes");
        displayShortBreakLabel.setText(settings.getShortBreakDuration().toMinutes() + " minutes");
        displayLongBreakLabel.setText(settings.getLongBreakDuration().toMinutes() + " minutes");
        displayRoundsLabel.setText(String.valueOf(settings.getRoundsBeforeLongBreak()));
    }

    private void applyCustomSettings() {
        if (!customModeButton.isSelected()) return;
        validateAllCustomFields();
        if (isFocusValid.get() && isShortBreakValid.get() && isLongBreakValid.get() && isRoundsValid.get()) {
            try {
                int focusMin = Integer.parseInt(customFocusField.getText());
                int shortBreakMin = Integer.parseInt(customShortBreakField.getText());
                int longBreakMin = Integer.parseInt(customLongBreakField.getText());
                int rounds = Integer.parseInt(customRoundsField.getText());
                pomodoroService.applyCustomSettings(focusMin, shortBreakMin, longBreakMin, rounds);
            } catch (NumberFormatException e) {
                System.err.println("Invalid custom settings input despite validation.");
            }
        }
    }
}