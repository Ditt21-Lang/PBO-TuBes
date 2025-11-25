package com.pomodone.facade;

import com.pomodone.service.PomodoroService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;

public class PomodoroFacade {
    private final PomodoroService pomodoroService;

    public PomodoroFacade() {
        this.pomodoroService = PomodoroService.getInstance();
    }

    public void selectMode(PomodoroService.PomodoroMode mode) {
        pomodoroService.selectMode(mode);
    }

    public void applyCustomSettings(int focus, int sBreak, int lBreak, int rounds) {
        pomodoroService.applyCustomSettings(focus, sBreak, lBreak, rounds);
    }

    public void handleStartPause() {
        pomodoroService.handleStartPause();
    }

    public void stopAndResetTimer() {
        pomodoroService.stopAndResetTimer();
    }

    public ReadOnlyStringProperty hoursProperty() {
        return pomodoroService.hoursProperty();
    }

    public ReadOnlyStringProperty minutesProperty() {
        return pomodoroService.minutesProperty();
    }

    public ReadOnlyStringProperty secondsProperty() {
        return pomodoroService.secondsProperty();
    }

    public ReadOnlyBooleanProperty showHoursProperty() {
        return pomodoroService.showHoursProperty();
    }

    public ReadOnlyStringProperty statusStringProperty() {
        return pomodoroService.statusStringProperty();
    }

    public ReadOnlyObjectProperty<PomodoroService.TimerState> timerStateProperty() {
        return pomodoroService.timerStateProperty();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return pomodoroService.progressProperty();
    }
}