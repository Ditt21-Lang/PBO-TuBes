package com.pomodone.view;

import com.pomodone.model.pomodoro.PomodoroSettings;
import com.pomodone.service.PomodoroService;
import com.pomodone.strategy.pomodoro.ClassicPomodoroStrategy;
import com.pomodone.strategy.pomodoro.IntensePomodoroStrategy;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PomodoroController {

    // FXML Elements
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

    // Settings Panes
    @FXML private GridPane displayGridPane;
    @FXML private GridPane inputFieldsGridPane;

    // Display Labels
    @FXML private Label displayFocusLabel;
    @FXML private Label displayShortBreakLabel;
    @FXML private Label displayLongBreakLabel;
    @FXML private Label displayRoundsLabel;

    // Input Fields
    @FXML private TextField customFocusField;
    @FXML private TextField customShortBreakField;
    @FXML private TextField customLongBreakField;
    @FXML private TextField customRoundsField;

    private PomodoroService pomodoroService;

    @FXML
    public void initialize() {
        this.pomodoroService = PomodoroService.getInstance();
        bindUIToService();
        setupActionHandlers();
        updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
    }

    private void bindUIToService() {
        statusLabel.textProperty().bind(pomodoroService.statusStringProperty());
        hoursLabel.textProperty().bind(pomodoroService.hoursProperty());
        minutesLabel.textProperty().bind(pomodoroService.minutesProperty());
        secondsLabel.textProperty().bind(pomodoroService.secondsProperty());
        progressBar.progressProperty().bind(pomodoroService.progressProperty());
        
        hoursGroup.visibleProperty().bind(pomodoroService.showHoursProperty());
        hoursGroup.managedProperty().bind(pomodoroService.showHoursProperty());
        separatorLabel1.visibleProperty().bind(pomodoroService.showHoursProperty());
        separatorLabel1.managedProperty().bind(pomodoroService.showHoursProperty());

        pomodoroService.timerStateProperty().addListener((obs, oldState, newState) -> {
            boolean isStopped = newState == PomodoroService.TimerState.STOPPED;
            modeSelectionBox.setDisable(!isStopped);
            inputFieldsGridPane.setDisable(!isStopped); // Disable editing custom fields when timer is running

            switch (newState) {
                case STOPPED: startButton.setText("Start"); break;
                case RUNNING: startButton.setText("Pause"); break;
                case PAUSED: startButton.setText("Resume"); break;
            }
        });
    }

    private void setupActionHandlers() {
        startButton.setOnAction(event -> pomodoroService.handleStartPause());
        stopButton.setOnAction(event -> pomodoroService.stopAndResetTimer());

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            
            if (newToggle == classicModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
                pomodoroService.selectMode(PomodoroService.PomodoroMode.CLASSIC);
            } else if (newToggle == intenseModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.INTENSE);
                pomodoroService.selectMode(PomodoroService.PomodoroMode.INTENSE);
            } else if (newToggle == customModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CUSTOM);
                applyCustomSettings();
            }
        });

        customFocusField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) applyCustomSettings(); });
        customShortBreakField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) applyCustomSettings(); });
        customLongBreakField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) applyCustomSettings(); });
        customRoundsField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) applyCustomSettings(); });
    }
    
    private void updateSettingsView(PomodoroService.PomodoroMode mode) {
        PomodoroSettings settings;
        boolean isCustom = mode == PomodoroService.PomodoroMode.CUSTOM;

        displayGridPane.setVisible(!isCustom);
        displayGridPane.setManaged(!isCustom);
        inputFieldsGridPane.setVisible(isCustom);
        inputFieldsGridPane.setManaged(isCustom);
        
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
            case CUSTOM:
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
        try {
            int focusMin = Integer.parseInt(customFocusField.getText());
            int shortBreakMin = Integer.parseInt(customShortBreakField.getText());
            int longBreakMin = Integer.parseInt(customLongBreakField.getText());
            int rounds = Integer.parseInt(customRoundsField.getText());
            
            pomodoroService.applyCustomSettings(focusMin, shortBreakMin, longBreakMin, rounds);
        } catch (NumberFormatException e) {
            System.err.println("Invalid custom settings input.");
        }
    }
}
