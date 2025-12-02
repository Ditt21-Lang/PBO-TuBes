package com.pomodone.service;

import com.pomodone.model.dashboard.DashboardStats;
import com.pomodone.model.user.User;
import com.pomodone.repository.TaskRepository;

import java.util.Optional;

public class DashboardStatsService {
    private final PomodoroSessionService pomodoroSessionService;
    private final TaskRepository taskRepository;
    private final UserSettingsService userSettingsService;

    public DashboardStatsService() {
        this.pomodoroSessionService = new PomodoroSessionService();
        this.taskRepository = new TaskRepository();
        this.userSettingsService = new UserSettingsService();
    }

    public DashboardStats loadStats() {
        int dailySessions = pomodoroSessionService.getTodayCompletedSessions();
        int activeTasks = taskRepository.countActiveTasks();

        Optional<User> user = userSettingsService.getCurrentUserSettings();
        int dailyTarget = user.map(User::getDailyPomodoroTarget).orElse(0);

        int productivity = calculateProductivityPercent(dailySessions, dailyTarget);

        return new DashboardStats(dailySessions, dailyTarget, activeTasks, productivity);
    }

    private int calculateProductivityPercent(int dailySessions, int dailyTarget) {
        int completedTasks = taskRepository.countCompletedTasks();
        int completedOnTime = taskRepository.countCompletedOnTimeTasks();

        double pomodoroRatio;
        if (dailyTarget > 0) {
            pomodoroRatio = Math.min(1.0, (double) dailySessions / dailyTarget);
        } else {
            pomodoroRatio = dailySessions > 0 ? 1.0 : 0.0;
        }

        double onTimeRatio = completedTasks > 0 ? (double) completedOnTime / completedTasks : 0.0;

        double blended = (pomodoroRatio + onTimeRatio) / 2.0;
        return (int) Math.round(blended * 100);
    }
}
