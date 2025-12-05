package com.pomodone.facade;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.service.TaskService;

//Facade yang digunakan untuk TAsk
public class TaskManagementFacade {
    private final TaskService taskService;
    private static final Logger log = LoggerFactory.getLogger(TaskManagementFacade.class);

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
        log.info("Menyimpan perubahan task {}", id);
        taskService.updateTask(id, newTitle, newDescription, newDueDate, newDifficulty, newStatus);

    }
}
