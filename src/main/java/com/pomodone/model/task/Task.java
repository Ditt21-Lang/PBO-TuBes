package com.pomodone.model.task;

import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private final long id;
    private final String title;
    private final String description;
    private final LocalDateTime dueDate;
    private final TaskDifficulty difficulty;
    private final TaskStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    Task(TaskBuilder builder) {
        this.id = builder.id;
        this.title = Objects.requireNonNull(builder.title, "title wajib diisi");
        this.description = builder.description;
        this.dueDate = builder.dueDate;
        this.difficulty = Objects.requireNonNull(builder.difficulty, "difficulty wajib diisi");
        this.status = resolveStatus(builder);
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static TaskBuilder builder() {
        return new TaskBuilder();
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public TaskDifficulty getDifficulty() {
        return difficulty;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Task withStatus(TaskStatus newStatus) {
        return Task.builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .dueDate(this.dueDate)
                .difficulty(this.difficulty)
                .status(newStatus)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Task withUpdatedFields(Task updated) {
        return Task.builder()
                .id(this.id)
                .title(Objects.requireNonNullElse(updated.title, this.title))
                .description(Objects.requireNonNullElse(updated.description, this.description))
                .dueDate(updated.dueDate != null ? updated.dueDate : this.dueDate)
                .difficulty(updated.difficulty != null ? updated.difficulty : this.difficulty)
                .status(updated.status != null ? updated.status : this.status)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TaskStatus resolveStatus(TaskBuilder builder) {
        TaskStatus initialStatus = Objects.requireNonNull(builder.status, "status wajib diisi");
        if (builder.dueDate == null) {
            return initialStatus;
        }
        if (!initialStatus.isFinished() && builder.dueDate.isBefore(LocalDateTime.now())) {
            return TaskStatus.TERLAMBAT;
        }
        return initialStatus;
    }

    

    
}
