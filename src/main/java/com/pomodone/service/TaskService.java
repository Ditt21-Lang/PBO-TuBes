package com.pomodone.service;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskService {
    private final TaskRepository taskrepository;

    public TaskService() {
        this.taskrepository = new TaskRepository();
    }

    public void createNewTask(String title, String description, LocalDateTime duedate, TaskDifficulty difficulty) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Judul tugas tidak boleh kosong!");
        }

        Task newTask = Task.builder()
                         .title(title)
                         .description(description)
                         .dueDate(duedate)
                         .difficulty(difficulty != null ? difficulty : TaskDifficulty.SEDANG)
                         .status(TaskStatus.BELUM_SELESAI)
                         .build();
        
        taskrepository.save(newTask);
    }

    public Task getTaskDetail(String title) {
        return taskrepository.findByTitle(title)
                .orElseThrow(() -> new IllegalArgumentException("Tugas tidak ditemukan: " + title));
    }

    public List<Task> getAllTasks() {
        return taskrepository.findAll();
    }
}
