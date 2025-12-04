package com.pomodone.service;

import com.pomodone.repository.PomodoroSessionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class PomodoroSessionService {
    private final PomodoroSessionRepository repository;
    private static final long CURRENT_USER_ID = 1;

    public PomodoroSessionService() {
        this.repository = new PomodoroSessionRepository();
    }

    public void logCompletedSession(LocalDateTime startedAt, LocalDateTime endedAt, long durationSeconds, PomodoroService.PomodoroMode mode) {
        if (startedAt == null || endedAt == null) return;
        repository.insertCompletedSession(CURRENT_USER_ID, startedAt, endedAt, durationSeconds, mode);
    }

    public int getTodayCompletedSessions() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return repository.countSessionsSince(CURRENT_USER_ID, startOfDay);
    }

    public int getCurrentWeekCompletedSessions() {
        LocalDate today = LocalDate.now();
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate startOfWeek = today.with(wf.dayOfWeek(), 1);
        return repository.countSessionsSince(CURRENT_USER_ID, startOfWeek.atStartOfDay());
    }
}
