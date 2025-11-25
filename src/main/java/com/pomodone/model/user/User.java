package com.pomodone.model.user;

public class User {
    private long id;
    private String name;
    private int dailyPomodoroTarget;
    private int weeklyPomodoroTarget;

    public User() {}

    public User(long id, String name, int dailyPomodoroTarget, int weeklyPomodoroTarget) {
        this.id = id;
        this.name = name;
        this.dailyPomodoroTarget = dailyPomodoroTarget;
        this.weeklyPomodoroTarget = weeklyPomodoroTarget;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDailyPomodoroTarget() {
        return dailyPomodoroTarget;
    }

    public int getWeeklyPomodoroTarget() {
        return weeklyPomodoroTarget;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDailyPomodoroTarget(int dailyPomodoroTarget) {
        this.dailyPomodoroTarget = dailyPomodoroTarget;
    }

    public void setWeeklyPomodoroTarget(int weeklyPomodoroTarget) {
        this.weeklyPomodoroTarget = weeklyPomodoroTarget;
    }
}
