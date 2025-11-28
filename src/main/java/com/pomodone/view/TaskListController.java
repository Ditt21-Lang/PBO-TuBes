package com.pomodone.view;

import com.pomodone.model.task.Task;
import com.pomodone.service.TaskService;
import com.pomodone.model.task.TaskStatus;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.util.List;


public class TaskListController {

    
    @FXML
    private ListView<String> taskListView; 
    @FXML
    private VBox detailContainer;
    @FXML
    private Label detailTitleLabel; 
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailDeadlineLabel; 
    @FXML
    private TextArea detailDescriptionArea; 

    private TaskService taskService;

    @FXML
    public void initialize() {
        this.taskService = new TaskService();

        setupListListener();
        loadTaskFromDatabase(); 
    }

    private void setupListListener() {
        taskListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showTaskDetail(newValue);
            }
        });
    }

    private void showTaskDetail(String title) {
        try {
            Task task = taskService.getTaskDetail(title);

            // Update Tampilan UI
            detailTitleLabel.setText(task.getTitle());
            detailStatusLabel.setText(task.getStatus().toString());
            
            if (task.getStatus() == TaskStatus.TERLAMBAT) {
                detailStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); 
            } else if (task.getStatus() == TaskStatus.SELESAI) {
                detailStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
            } else {
                detailStatusLabel.setStyle("-fx-text-fill: -color-accent; -fx-font-weight: bold;"); 
            }   

            if (task.getDueDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
                String formattedDate = task.getDueDate().format(formatter);

                detailDeadlineLabel.setText(formattedDate);
            } else {
                detailDeadlineLabel.setText("-");
            }

            detailDescriptionArea.setText(task.getDescription());
            detailContainer.setVisible(true);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            detailDescriptionArea.setText("Gagal memuat detail tugas.");
        }
    }

    private void loadTaskFromDatabase() {
        try {
            List<Task> allTasks = taskService.getAllTasks();

            taskListView.getItems().clear();

            for (Task task : allTasks) {
                taskListView.getItems().add(task.getTitle());
            }

            if (allTasks.isEmpty()) {
                taskListView.setPlaceholder(new Label("Belum ada tugas. Tambah baru yuk!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Gagal load data dari DB: " + e.getMessage());
        }
        
    }
}