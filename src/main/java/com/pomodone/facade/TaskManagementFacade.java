package com.pomodone.facade;

import com.pomodone.service.TaskService; 
import com.pomodone.model.task.TaskDifficulty;

import java.time.LocalDateTime; 

public class TaskManagementFacade {
    private final TaskService taskService;

    public TaskManagementFacade() {
        this.taskService = new TaskService();
    }

    public void addTask(String title, String description, LocalDateTime deadline, TaskDifficulty difficulty) {
        taskService.createNewTask(title, description, deadline, difficulty);
    }
}
