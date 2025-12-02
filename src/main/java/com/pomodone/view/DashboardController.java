package com.pomodone.view;

import com.pomodone.model.dashboard.DashboardStats;
import com.pomodone.service.DashboardStatsService;
import com.pomodone.service.TaskService;
import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    @FXML private Label dailyPomodoroLabel;
    @FXML private Label activeTasksLabel;
    @FXML private Label productivityLabel;
    @FXML private Label headerSubtitle;
    @FXML private VBox priorityTasksBox;
    @FXML private TextField dashboardSearchField;

    private final DashboardStatsService statsService = new DashboardStatsService();
    private final TaskService taskService = new TaskService();
    private final DateTimeFormatter dueFormatter = DateTimeFormatter.ofPattern("dd MMM HH:mm", new Locale("id", "ID"));

    @FXML
    public void initialize() {
        loadStats();
        loadPriorityTasks();
        setupSearchHandler();
    }

    private void loadStats() {
        DashboardStats stats = statsService.loadStats();
        dailyPomodoroLabel.setText(formatPomodoroProgress(stats));
        activeTasksLabel.setText(String.valueOf(stats.getActiveTasks()));
        productivityLabel.setText(stats.getProductivityPercent() + "%");
        headerSubtitle.setText("Welcome back, let's be productive!");
    }

    private void setupSearchHandler() {
        if (dashboardSearchField == null) return;
        dashboardSearchField.setOnAction(event -> triggerTaskListSearch());
    }

    private void triggerTaskListSearch() {
        String query = dashboardSearchField.getText();
        MainWindowController mainWindow = MainWindowController.getInstance();
        if (mainWindow != null) {
            mainWindow.navigateToTaskListWithSearch(query);
        }
    }

    private void loadPriorityTasks() {
        priorityTasksBox.getChildren().clear();
        List<Task> tasks = taskService.getTopByDueDate(5);
        if (tasks.isEmpty()) {
            Label empty = new Label("No priority tasks.");
            empty.getStyleClass().add("task-item-due-date");
            priorityTasksBox.getChildren().add(empty);
            return;
        }

        for (Task task : tasks) {
            priorityTasksBox.getChildren().add(createTaskRow(task));
        }
    }

    private HBox createTaskRow(Task task) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("task-item-title");

        Label difficulty = new Label(formatDifficulty(task.getDifficulty()));
        difficulty.getStyleClass().add(resolveDifficultyClass(task.getDifficulty()));

        Label due = new Label(formatDue(task));
        due.getStyleClass().add("task-item-due-date");
        if (task.getStatus() == TaskStatus.TERLAMBAT) {
            due.setStyle("-fx-text-fill: #DC2626;");
        }

        VBox info = new VBox(title, difficulty);
        info.setSpacing(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(info, spacer, due);
        row.setSpacing(8);
        row.getStyleClass().add("task-item");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));
        return row;
    }

    private String formatDifficulty(TaskDifficulty difficulty) {
        return switch (difficulty) {
            case SULIT -> "High Priority";
            case SEDANG -> "Medium Priority";
            case MUDAH -> "Low Priority";
        };
    }

    private String resolveDifficultyClass(TaskDifficulty difficulty) {
        return switch (difficulty) {
            case SULIT -> "task-item-priority-high";
            case SEDANG -> "task-item-priority-medium";
            case MUDAH -> "task-item-priority-low";
        };
    }

    private String formatDue(Task task) {
        if (task.getDueDate() == null) return "No deadline";
        return "Due: " + dueFormatter.format(task.getDueDate());
    }

    private String formatPomodoroProgress(DashboardStats stats) {
        int target = Math.max(stats.getDailyPomodoroTarget(), 0);
        int done = Math.max(stats.getDailyPomodoroDone(), 0);
        if (target > 0) {
            return done + " / " + target;
        }
        return String.valueOf(done);
    }
}
