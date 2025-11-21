package com.pomodone.model.task;

public enum TaskStatus {
    BELUM_SELESAI,
    TERLAMBAT,
    SELESAI;

    public boolean isFinished() {
        return this == SELESAI;
    }

    public boolean isPending() {
        return this == BELUM_SELESAI || this == TERLAMBAT;
    }
}
