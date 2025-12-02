package com.pomodone.service;

import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import com.pomodone.repository.PomodoroPresetRepository;

public class CustomPomodoroPresetService {
    private final PomodoroPresetRepository repository;
    private final long currentUserId = 1; // TODO: ganti kalau sudah ada login

    public CustomPomodoroPresetService() {
        this.repository = new PomodoroPresetRepository();
    }

    public CustomPomodoroPreset loadLatestPreset() {
        return repository.findLatestByUser(currentUserId);
    }

    public void savePreset(int focus, int shortBreak, int longBreak, int rounds) {
        CustomPomodoroPreset preset = new CustomPomodoroPreset(
                0,
                currentUserId,
                "Custom",
                focus,
                shortBreak,
                longBreak,
                rounds
        );
        repository.upsert(currentUserId, preset);
    }
}
