package com.pomodone.view;

import com.pomodone.facade.PomodoroFacade;
import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import com.pomodone.model.pomodoro.PomodoroSettings;
import com.pomodone.service.PomodoroService;
import com.pomodone.service.CustomPomodoroPresetService;
import com.pomodone.strategy.pomodoro.ClassicPomodoroStrategy;
import com.pomodone.strategy.pomodoro.IntensePomodoroStrategy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PomodoroController {
    private static final Logger log = LoggerFactory.getLogger(PomodoroController.class);

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

    private PomodoroFacade pomodoroFacade;
    private CustomPomodoroPresetService presetService;

    // buat validasi
    private final BooleanProperty isFocusValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isShortBreakValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isLongBreakValid = new SimpleBooleanProperty(true);
    private final BooleanProperty isRoundsValid = new SimpleBooleanProperty(true);

    @FXML
    public void initialize() {
        this.pomodoroFacade = new PomodoroFacade();
        this.presetService = new CustomPomodoroPresetService();
        loadCustomPresetDefaults();
        bindUIToFacade();
        setupActionHandlers();
        setupValidationListeners();
        updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
    }

    private void bindUIToFacade() {
        // sambungin tampilan ke service
        statusLabel.textProperty().bind(pomodoroFacade.statusStringProperty());
        hoursLabel.textProperty().bind(pomodoroFacade.hoursProperty());
        minutesLabel.textProperty().bind(pomodoroFacade.minutesProperty());
        secondsLabel.textProperty().bind(pomodoroFacade.secondsProperty());
        progressBar.progressProperty().bind(pomodoroFacade.progressProperty());
        
        // atur visibility jam
        hoursGroup.visibleProperty().bind(pomodoroFacade.showHoursProperty());
        hoursGroup.managedProperty().bind(pomodoroFacade.showHoursProperty());
        separatorLabel1.visibleProperty().bind(pomodoroFacade.showHoursProperty());
        separatorLabel1.managedProperty().bind(pomodoroFacade.showHoursProperty());

        pomodoroFacade.timerStateProperty().addListener((obs, oldState, newState) -> {
            boolean isStopped = newState == PomodoroService.TimerState.STOPPED;
            modeSelectionBox.setDisable(!isStopped);
            inputFieldsGridPane.setDisable(!isStopped);

            switch (newState) {
                case STOPPED: startButton.setText("Start"); break;
                case RUNNING: startButton.setText("Pause"); break;
                case PAUSED: startButton.setText("Resume"); break;
            }
        });
        
        // dengerin perubahan validasi buat aktifin/nonaktifin tombol start di mode custom
        isFocusValid.addListener((obs, o, n) -> updateStartButtonState());
        isShortBreakValid.addListener((obs, o, n) -> updateStartButtonState());
        isLongBreakValid.addListener((obs, o, n) -> updateStartButtonState());
        isRoundsValid.addListener((obs, o, n) -> updateStartButtonState());
        customModeButton.selectedProperty().addListener((obs, o, n) -> updateStartButtonState());
    }

    private void setupActionHandlers() {
        startButton.setOnAction(event -> {
            PomodoroService.TimerState state = pomodoroFacade.timerStateProperty().get();
            boolean isCustom = customModeButton.isSelected();

            // Hanya apply/persist custom ketika start dari kondisi STOPPED,
            // supaya pause/resume tidak mereset timer.
            if (isCustom && state == PomodoroService.TimerState.STOPPED) {
                validateAllCustomFields();
                if (!areAllCustomFieldsValid()) {
                    return;
                }
                applyCustomSettings();
                persistCustomPreset();
            }
            pomodoroFacade.handleStartPause();
        });
        stopButton.setOnAction(event -> pomodoroFacade.stopAndResetTimer());

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true); return;
            }
            if (newToggle == classicModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CLASSIC);
                pomodoroFacade.selectMode(PomodoroService.PomodoroMode.CLASSIC);
            } else if (newToggle == intenseModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.INTENSE);
                pomodoroFacade.selectMode(PomodoroService.PomodoroMode.INTENSE);
            } else if (newToggle == customModeButton) {
                updateSettingsView(PomodoroService.PomodoroMode.CUSTOM);
                validateAllCustomFields(); // validasi sekali pas ganti
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
            toggleError(field, errorLabel, true, "Gaboleh kosong.");
            return false;
        }
        if (value.contains(".") || value.contains(",")) {
            toggleError(field, errorLabel, true, "Gaboleh desimal.");
            return false;
        }
        try {
            int intValue = Integer.parseInt(value);
            if (allowZero ? intValue < 0 : intValue <= 0) {
                toggleError(field, errorLabel, true, "Harus positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            toggleError(field, errorLabel, true, "Harus angka valid.");
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

    private boolean areAllCustomFieldsValid() {
        return isFocusValid.get() && isShortBreakValid.get() && isLongBreakValid.get() && isRoundsValid.get();
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
                pomodoroFacade.applyCustomSettings(focusMin, shortBreakMin, longBreakMin, rounds);
            } catch (NumberFormatException e) {
                log.warn("Input custom settings tidak valid meski lolos validasi awal");
            }
        }
    }

    private void persistCustomPreset() {
        try {
            int focusMin = Integer.parseInt(customFocusField.getText());
            int shortBreakMin = Integer.parseInt(customShortBreakField.getText());
            int longBreakMin = Integer.parseInt(customLongBreakField.getText());
            int rounds = Integer.parseInt(customRoundsField.getText());
            presetService.savePreset(focusMin, shortBreakMin, longBreakMin, rounds);
        } catch (NumberFormatException ignored) {
            // sudah divalidasi
        }
    }

    private void loadCustomPresetDefaults() {
        CustomPomodoroPreset preset = presetService.loadLatestPreset();
        if (preset != null) {
            customFocusField.setText(String.valueOf(preset.getFocusMinutes()));
            customShortBreakField.setText(String.valueOf(preset.getShortBreakMinutes()));
            customLongBreakField.setText(String.valueOf(preset.getLongBreakMinutes()));
            customRoundsField.setText(String.valueOf(preset.getRounds()));
        } else {
            customFocusField.setText("25");
            customShortBreakField.setText("5");
            customLongBreakField.setText("15");
            customRoundsField.setText("4");
        }
    }
}
