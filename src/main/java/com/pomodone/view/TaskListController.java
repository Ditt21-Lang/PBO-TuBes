package com.pomodone.view;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.service.TaskService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskListController {

    @FXML private ListView<String> taskListView;
    @FXML private Button addTaskButton;
    
    @FXML private VBox detailContainer;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailDeadlineLabel;
    @FXML private TextArea detailDescriptionArea;

    private TaskService taskService;

    @FXML
    public void initialize() {
        this.taskService = new TaskService();

        setupListListener();
        setupAddButton();
        loadTaskFromDatabase();
    }

    private void setupAddButton() {
        addTaskButton.setOnAction(event -> showAddTaskDialog());
    }

    private void setupListListener() {
        taskListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showTaskDetail(newValue);
            }
        });
    }

    // --- LOGIKA MENAMPILKAN DIALOG INPUT ---
    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Create a new task to stay productive!");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Task Title");
        
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefHeight(100);

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());

        ComboBox<TaskDifficulty> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().setAll(TaskDifficulty.values());
        difficultyBox.setValue(TaskDifficulty.SEDANG);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Deadline:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Difficulty:"), 0, 3);
        grid.add(difficultyBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText();
                String desc = descField.getText();
                TaskDifficulty diff = difficultyBox.getValue();
                
                LocalDateTime dueDate = null;
                if (datePicker.getValue() != null) {
                    dueDate = datePicker.getValue().atTime(LocalTime.MAX); 
                }

                try {
                    taskService.createNewTask(title, desc, dueDate, diff);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal simpan: " + e.getMessage());
                    alert.show();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
        loadTaskFromDatabase();
    }

    private void showTaskDetail(String title) {
        try {
            Task task = taskService.getTaskDetail(title);

            detailTitleLabel.setText(task.getTitle());
            detailStatusLabel.setText(task.getStatus().toString());

            if (task.getStatus() == TaskStatus.TERLAMBAT) {
                detailStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            } else if (task.getStatus() == TaskStatus.SELESAI) {
                detailStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
            } else {
                detailStatusLabel.setStyle("-fx-text-fill: #06B6D4; -fx-font-weight: bold;");
            }

            if (task.getDueDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
                detailDeadlineLabel.setText(task.getDueDate().format(formatter));
            } else {
                detailDeadlineLabel.setText("-");
            }

            detailDescriptionArea.setText(task.getDescription());
            detailContainer.setVisible(true);

        } catch (IllegalArgumentException e) {
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
                taskListView.setPlaceholder(new Label("No tasks yet. Click '+ New' to add one!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}