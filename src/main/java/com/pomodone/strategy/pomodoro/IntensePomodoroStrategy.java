package com.pomodone.strategy.pomodoro;

import com.pomodone.model.pomodoro.PomodoroSettings;
import java.time.Duration;

public class IntensePomodoroStrategy implements PomodoroStrategy {
    private static final Duration FOCUS_DURATION = Duration.ofMinutes(50);
    private static final Duration SHORT_BREAK_DURATION = Duration.ofMinutes(10);
    private static final Duration LONG_BREAK_DURATION = Duration.ofMinutes(30);
    private static final int ROUNDS_BEFORE_LONG_BREAK = 2;

    @Override
    public PomodoroSettings getSettings() {
        return new PomodoroSettings(
                FOCUS_DURATION,
                SHORT_BREAK_DURATION,
                LONG_BREAK_DURATION,
                ROUNDS_BEFORE_LONG_BREAK
        );
    }
}
