package com.pomodone.model.task;

import java.time.LocalDateTime;

public class TaskBuilder {
    long id;
    String title;
    String description;
    LocalDateTime dueDate;
    TaskDifficulty difficulty = TaskDifficulty.SEDANG;
    TaskStatus status = TaskStatus.BELUM_SELESAI;
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    TaskBuilder() {
    }

    public TaskBuilder id(long id) {
        this.id = id;
        return this;
    }

    public TaskBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TaskBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TaskBuilder dueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public TaskBuilder difficulty(TaskDifficulty difficulty) {
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
        return this;
    }

    public TaskBuilder status(TaskStatus status) {
        if (status != null) {
            this.status = status;
        }
        return this;
    }

    public TaskBuilder createdAt(LocalDateTime createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
        return this;
    }

    public TaskBuilder updatedAt(LocalDateTime updatedAt) {
        if (updatedAt != null) {
            this.updatedAt = updatedAt;
        }
        return this;
    }

    public Task build() {
        return new Task(this);
    }
}
