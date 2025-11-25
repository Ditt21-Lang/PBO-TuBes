package com.pomodone.strategy.pomodoro;

import com.pomodone.model.pomodoro.PomodoroSettings;
import java.time.Duration;

public class ClassicPomodoroStrategy implements PomodoroStrategy {
    private static final Duration FOCUS_DURATION = Duration.ofMinutes(25);
    private static final Duration SHORT_BREAK_DURATION = Duration.ofMinutes(5);
    private static final Duration LONG_BREAK_DURATION = Duration.ofMinutes(15);
    private static final int ROUNDS_BEFORE_LONG_BREAK = 4;

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
