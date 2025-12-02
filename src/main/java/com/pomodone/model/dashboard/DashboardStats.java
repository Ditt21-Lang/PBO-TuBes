package com.pomodone.model.dashboard;

public class DashboardStats {
    private final int dailyPomodoroDone;
    private final int dailyPomodoroTarget;
    private final int activeTasks;
    private final int productivityPercent;

    public DashboardStats(int dailyPomodoroDone, int dailyPomodoroTarget, int activeTasks, int productivityPercent) {
        this.dailyPomodoroDone = dailyPomodoroDone;
        this.dailyPomodoroTarget = dailyPomodoroTarget;
        this.activeTasks = activeTasks;
        this.productivityPercent = productivityPercent;
    }

    public int getDailyPomodoroDone() {
        return dailyPomodoroDone;
    }

    public int getDailyPomodoroTarget() {
        return dailyPomodoroTarget;
    }

    public int getActiveTasks() {
        return activeTasks;
    }

    public int getProductivityPercent() {
        return productivityPercent;
    }
}
