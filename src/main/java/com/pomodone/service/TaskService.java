package com.pomodone.service;

import java.time.LocalDateTime;
import java.util.List;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.repository.TaskRepository;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService() {
        this(new TaskRepository());
    }

    TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void createNewTask(String title, String description, LocalDateTime duedate, TaskDifficulty difficulty) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty!");
        }

        Task newTask = Task.builder()
                         .title(title)
                         .description(description)
                         .dueDate(duedate)
                         .difficulty(difficulty != null ? difficulty : TaskDifficulty.SEDANG)
                         .status(TaskStatus.BELUM_SELESAI)
                         .build();
        
        taskRepository.save(newTask);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    public List<Task> getTopByDueDate(int limit) {
        return taskRepository.findTopByDueDate(limit);
    }

    public void deleteTask (int id){
        if (id <= 0){
            throw new IllegalArgumentException ("Task title not found. Failed to delete task!");
        } 

        taskRepository.delete(id);
    }

    public void updateTask(long id, String newTitle, String newDescription,
                       LocalDateTime newDueDate, TaskDifficulty newDifficulty,
                       TaskStatus newStatus) {
   
        Task existingTask = taskRepository.findById(id);
        if (existingTask == null) {
            throw new IllegalArgumentException("Task dengan ID " + id + " tidak ditemukan!");
        }

        Task updateRequest = Task.builder()
                .id(id)
                .title(newTitle)                
                .description(newDescription)    
                .dueDate(newDueDate)            
                .difficulty(newDifficulty)      
                .status(newStatus)              
                .build();

        Task updatedTask = existingTask.withUpdatedFields(updateRequest);

        taskRepository.update(updatedTask);
    }

}
