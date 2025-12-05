package com.pomodone.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pomodone.facade.PomodoroFacade;
import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import com.pomodone.model.pomodoro.PomodoroSettings;
import com.pomodone.service.CustomPomodoroPresetService;
import com.pomodone.service.PomodoroService;
import com.pomodone.strategy.pomodoro.ClassicPomodoroStrategy;
import com.pomodone.strategy.pomodoro.IntensePomodoroStrategy;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PomodoroController {
    private static final Logger log = LoggerFactory.getLogger(PomodoroController.class);
    private static final String FIELD_ERROR_CLASS = "field-error";
    private static final String MINUTES_LABEL = " minutes";

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
                case STOPPED -> startButton.setText("Start");
                case RUNNING -> startButton.setText("Pause");
                case PAUSED -> startButton.setText("Resume");
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
        startButton.setOnAction(event -> handleStartButtonAction());
        stopButton.setOnAction(event -> pomodoroFacade.stopAndResetTimer());

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> handleModeToggleSelection(oldToggle, newToggle));
    }

    private void handleStartButtonAction() {
        PomodoroService.TimerState state = pomodoroFacade.timerStateProperty().get();

        // Hanya apply/persist custom ketika start dari kondisi STOPPED,
        // supaya pause/resume tidak mereset timer.
        if (isCustomStartFromStopped(state)) {
            validateAllCustomFields();
            if (!areAllCustomFieldsValid()) {
                return;
            }
            applyCustomSettings();
            persistCustomPreset();
        }
        pomodoroFacade.handleStartPause();
    }

    private boolean isCustomStartFromStopped(PomodoroService.TimerState state) {
        return customModeButton.isSelected() && state == PomodoroService.TimerState.STOPPED;
    }

    private void handleModeToggleSelection(Toggle oldToggle, Toggle newToggle) {
        if (newToggle == null && oldToggle != null) {
            oldToggle.setSelected(true);
            return;
        }
        PomodoroService.PomodoroMode mode = resolveMode(newToggle);
        updateSettingsView(mode);

        if (mode == PomodoroService.PomodoroMode.CUSTOM) {
            validateAllCustomFields(); // validasi sekali pas ganti
            applyCustomSettings();
            return;
        }
        pomodoroFacade.selectMode(mode);
    }

    private PomodoroService.PomodoroMode resolveMode(Toggle toggle) {
        if (toggle == classicModeButton) {
            return PomodoroService.PomodoroMode.CLASSIC;
        }
        if (toggle == intenseModeButton) {
            return PomodoroService.PomodoroMode.INTENSE;
        }
        return PomodoroService.PomodoroMode.CUSTOM;
    }

    private void updateStartButtonState() {
        boolean isCustomMode = customModeButton.isSelected();
        boolean areCustomFieldsInvalid = !isFocusValid.get() || !isShortBreakValid.get() || !isLongBreakValid.get() || !isRoundsValid.get();
        
        startButton.setDisable(isCustomMode && areCustomFieldsInvalid);
    }

    private void setupValidationListeners() {
        attachValidationListener(customFocusField, focusErrorLabel, isFocusValid, false);
        attachValidationListener(customShortBreakField, shortBreakErrorLabel, isShortBreakValid, true);
        attachValidationListener(customLongBreakField, longBreakErrorLabel, isLongBreakValid, true);
        attachValidationListener(customRoundsField, roundsErrorLabel, isRoundsValid, false);
    }

    private void attachValidationListener(TextField field, Label errorLabel, BooleanProperty validityProperty, boolean allowZero) {
        field.textProperty().addListener((obs, oldV, newV) -> {
            boolean isValid = validatePositiveInteger(newV, field, errorLabel, allowZero);
            validityProperty.set(isValid);
            if (isValid) {
                applyCustomSettings();
            }
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
            if (!field.getStyleClass().contains(FIELD_ERROR_CLASS)) field.getStyleClass().add(FIELD_ERROR_CLASS);
        } else {
            field.getStyleClass().remove(FIELD_ERROR_CLASS);
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
            case CLASSIC -> {
                settings = new ClassicPomodoroStrategy().getSettings();
                settingsTitleLabel.setText("Classic Settings");
                populateDisplayLabels(settings);
            }
            case INTENSE -> {
                settings = new IntensePomodoroStrategy().getSettings();
                settingsTitleLabel.setText("Intense Settings");
                populateDisplayLabels(settings);
            }
            default -> settingsTitleLabel.setText("Custom Settings");
        }
    }
    
    private void populateDisplayLabels(PomodoroSettings settings) {
        displayFocusLabel.setText(settings.getFocusDuration().toMinutes() + MINUTES_LABEL);
        displayShortBreakLabel.setText(settings.getShortBreakDuration().toMinutes() + MINUTES_LABEL);
        displayLongBreakLabel.setText(settings.getLongBreakDuration().toMinutes() + MINUTES_LABEL);
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
