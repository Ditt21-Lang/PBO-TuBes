package com.pomodone.service;

import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import com.pomodone.repository.PomodoroPresetRepository;

public class CustomPomodoroPresetService {
    private final PomodoroPresetRepository repository;
    private static final long CURRENT_USER_ID = 1;

    public CustomPomodoroPresetService() {
        this.repository = new PomodoroPresetRepository();
    }

    public CustomPomodoroPreset loadLatestPreset() {
        return repository.findLatestByUser(CURRENT_USER_ID);
    }

    public void savePreset(int focus, int shortBreak, int longBreak, int rounds) {
        CustomPomodoroPreset preset = new CustomPomodoroPreset(
                0,
                CURRENT_USER_ID,
                "Custom",
                focus,
                shortBreak,
                longBreak,
                rounds
        );
        repository.upsert(CURRENT_USER_ID, preset);
    }
}
