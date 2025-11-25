package com.pomodone.strategy.pomodoro;

import com.pomodone.model.pomodoro.PomodoroSettings;

public class CustomPomodoroStrategy implements PomodoroStrategy {

    private final PomodoroSettings settings;

    public CustomPomodoroStrategy(PomodoroSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Custom Pomodoro settings cannot be null.");
        }
        this.settings = settings;
    }

    @Override
    public PomodoroSettings getSettings() {
        return settings;
    }
}
