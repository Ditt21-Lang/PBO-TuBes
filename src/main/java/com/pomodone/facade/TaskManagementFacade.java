package com.pomodone.facade;

import java.time.LocalDateTime;

import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.service.TaskService; 

public class TaskManagementFacade {
    private final TaskService taskService;

    public TaskManagementFacade() {
        this.taskService = new TaskService();
    }

    public void addTask(String title, String description, LocalDateTime deadline, TaskDifficulty difficulty) {
        taskService.createNewTask(title, description, deadline, difficulty);
    }

    public void destroyTask(int id){
        taskService.deleteTask(id);
    }

    public void saveTask(long id, String newTitle, String newDescription,LocalDateTime newDueDate, TaskDifficulty newDifficulty, TaskStatus newStatus){
        System.out.println("SAVING TASK");
        taskService.updateTask(id, newTitle, newDescription, newDueDate, newDifficulty, newStatus);

    }
}
