package com.pomodone.model.pomodoro;

import java.time.Duration;

public class PomodoroSettings {
    private final Duration focusDuration;
    private final Duration shortBreakDuration;
    private final Duration longBreakDuration;
    private final int roundsBeforeLongBreak;

    public PomodoroSettings(Duration focusDuration, Duration shortBreakDuration, Duration longBreakDuration, int roundsBeforeLongBreak) {
        if (focusDuration == null || focusDuration.isNegative() || focusDuration.isZero()) throw new IllegalArgumentException("Focus duration must be positive.");
        if (shortBreakDuration == null || shortBreakDuration.isNegative()) throw new IllegalArgumentException("Short break duration cannot be negative.");
        if (longBreakDuration == null || longBreakDuration.isNegative()) throw new IllegalArgumentException("Long break duration cannot be negative.");
        if (roundsBeforeLongBreak <= 0) throw new IllegalArgumentException("Rounds must be positive.");

        this.focusDuration = focusDuration;
        this.shortBreakDuration = shortBreakDuration;
        this.longBreakDuration = longBreakDuration;
        this.roundsBeforeLongBreak = roundsBeforeLongBreak;
    }

    public Duration getFocusDuration() {
        return focusDuration;
    }

    public Duration getShortBreakDuration() {
        return shortBreakDuration;
    }

    public Duration getLongBreakDuration() {
        return longBreakDuration;
    }

    public int getRoundsBeforeLongBreak() {
        return roundsBeforeLongBreak;
    }
}