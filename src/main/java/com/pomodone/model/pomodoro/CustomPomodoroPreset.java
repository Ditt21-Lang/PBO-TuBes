package com.pomodone.model.pomodoro;

public class CustomPomodoroPreset {
    private final long id;
    private final long userId;
    private final String presetName;
    private final int focusMinutes;
    private final int shortBreakMinutes;
    private final int longBreakMinutes;
    private final int rounds;

    public CustomPomodoroPreset(long id, long userId, String presetName, int focusMinutes, int shortBreakMinutes, int longBreakMinutes, int rounds) {
        this.id = id;
        this.userId = userId;
        this.presetName = presetName;
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.rounds = rounds;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getPresetName() {
        return presetName;
    }

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public int getShortBreakMinutes() {
        return shortBreakMinutes;
    }

    public int getLongBreakMinutes() {
        return longBreakMinutes;
    }

    public int getRounds() {
        return rounds;
    }
}
