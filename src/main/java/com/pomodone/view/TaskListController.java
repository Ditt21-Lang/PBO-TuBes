package com.pomodone.view;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.service.TaskService;
import com.pomodone.util.CollectionViewProcessor;
import com.pomodone.util.SortDirection;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskListController {

    @FXML private ListView<Task> taskListView;
    @FXML private Button addTaskButton;
    @FXML private Button viewSettingsButton;
    @FXML private Button closeFilterPanelButton;
    @FXML private VBox filterPanel;
    @FXML private CheckBox pendingCheckBox;
    @FXML private CheckBox overdueCheckBox;
    @FXML private CheckBox doneCheckBox;
    @FXML private RadioButton sortNameAscRadio;
    @FXML private RadioButton sortNameDescRadio;
    @FXML private RadioButton sortDueSoonRadio;
    @FXML private RadioButton sortDueLateRadio;
    @FXML private RadioButton sortDifficultyDescRadio;
    private ToggleGroup sortToggleGroup;
    
    @FXML private VBox detailContainer;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailDeadlineLabel;
    @FXML private TextArea detailDescriptionArea;

    private TaskService taskService;
    private final CollectionViewProcessor<Task> viewProcessor = new CollectionViewProcessor<>();
    private final DateTimeFormatter deadlineFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
    private List<Task> allTasks = new ArrayList<>();

    @FXML
    public void initialize() {
        this.taskService = new TaskService();

        setupListViewCellFactory();
        setupListListener();
        setupAddButton();
        setupFilterControls();
        setupSortControls();
        loadTaskFromDatabase();
    }

    private void setupAddButton() {
        addTaskButton.setOnAction(event -> showAddTaskDialog());
    }

    private void setupListListener() {
        taskListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showTaskDetail(newValue);
            } else {
                detailContainer.setVisible(false);
            }
        });
    }

    private void setupListViewCellFactory() {
        taskListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                } else {
                    setText(formatTaskLabel(task));
                }
            }
        });
    }

    private String formatTaskLabel(Task task) {
        String statusTag = switch (task.getStatus()) {
            case TERLAMBAT -> "[Late]";
            case SELESAI -> "[Done]";
            default -> "[Open]";
        };
        return statusTag + " " + task.getTitle();
    }

    private void setupFilterControls() {
        pendingCheckBox.setSelected(true);
        overdueCheckBox.setSelected(true);
        doneCheckBox.setSelected(false);

        pendingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> handleFilterChange());
        overdueCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> handleFilterChange());
        doneCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> handleFilterChange());

        viewSettingsButton.setOnAction(event -> toggleFilterPanel());
        closeFilterPanelButton.setOnAction(event -> setFilterPanelVisible(false));
        setFilterPanelVisible(false);
    }

    private void setupSortControls() {
        sortToggleGroup = new ToggleGroup();
        sortNameAscRadio.setToggleGroup(sortToggleGroup);
        sortNameDescRadio.setToggleGroup(sortToggleGroup);
        sortDueSoonRadio.setToggleGroup(sortToggleGroup);
        sortDueLateRadio.setToggleGroup(sortToggleGroup);
        sortDifficultyDescRadio.setToggleGroup(sortToggleGroup);

        // default: due date terdekat lebih dulu
        sortDueSoonRadio.setSelected(true);
        sortToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> refreshTaskList());
    }

    private Comparator<Task> dueDateComparator() {
        return Comparator.comparing(
                Task::getDueDate,
                Comparator.nullsLast(LocalDateTime::compareTo)
        );
    }

    private Comparator<Task> titleComparator() {
        return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private Comparator<Task> difficultyComparator() {
        return Comparator.comparingInt(task -> mapDifficulty(task.getDifficulty()));
    }

    private int mapDifficulty(TaskDifficulty difficulty) {
        return switch (difficulty) {
            case MUDAH -> 1;
            case SEDANG -> 2;
            case SULIT -> 3;
        };
    }

    private void handleFilterChange() {
        enforceAtLeastOneStatusSelected();
        refreshTaskList();
    }

    private void enforceAtLeastOneStatusSelected() {
        if (!pendingCheckBox.isSelected() && !overdueCheckBox.isSelected() && !doneCheckBox.isSelected()) {
            pendingCheckBox.setSelected(true);
        }
    }

    private void toggleFilterPanel() {
        setFilterPanelVisible(!filterPanel.isVisible());
    }

    private void setFilterPanelVisible(boolean visible) {
        filterPanel.setVisible(visible);
        filterPanel.setManaged(visible);
    }

    private SortChoice resolveSortChoice() {
        if (sortNameAscRadio.isSelected()) return SortChoice.NAME_ASC;
        if (sortNameDescRadio.isSelected()) return SortChoice.NAME_DESC;
        if (sortDueSoonRadio.isSelected()) return SortChoice.DUE_DATE_ASC;
        if (sortDueLateRadio.isSelected()) return SortChoice.DUE_DATE_DESC;
        if (sortDifficultyDescRadio.isSelected()) return SortChoice.DIFFICULTY_DESC;
        // default fallback
        return SortChoice.DUE_DATE_ASC;
    }

    private Comparator<Task> resolveComparator(SortChoice choice) {
        return switch (choice) {
            case NAME_ASC, NAME_DESC -> titleComparator();
            case DUE_DATE_ASC, DUE_DATE_DESC -> dueDateComparator();
            case DIFFICULTY_DESC -> difficultyComparator();
        };
    }

    private SortDirection resolveDirection(SortChoice choice) {
        return switch (choice) {
            case NAME_ASC, DUE_DATE_ASC -> SortDirection.ASC;
            case NAME_DESC, DUE_DATE_DESC, DIFFICULTY_DESC -> SortDirection.DESC;
        };
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

    private void showTaskDetail(Task task) {
        if (task == null) {
            detailContainer.setVisible(false);
            return;
        }

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
            detailDeadlineLabel.setText(task.getDueDate().format(deadlineFormatter));
        } else {
            detailDeadlineLabel.setText("-");
        }

        detailDescriptionArea.setText(task.getDescription());
        detailContainer.setVisible(true);
    }

    private void loadTaskFromDatabase() {
        try {
            allTasks = taskService.getAllTasks();
            refreshTaskList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean statusSelected(Task task) {
        return (pendingCheckBox.isSelected() && task.getStatus() == TaskStatus.BELUM_SELESAI)
                || (overdueCheckBox.isSelected() && task.getStatus() == TaskStatus.TERLAMBAT)
                || (doneCheckBox.isSelected() && task.getStatus() == TaskStatus.SELESAI);
    }

    private void refreshTaskList() {
        SortChoice sortChoice = resolveSortChoice();
        Comparator<Task> comparator = resolveComparator(sortChoice);
        SortDirection direction = resolveDirection(sortChoice);

        List<Task> processed = viewProcessor.apply(
                allTasks,
                this::statusSelected,
                comparator,
                direction
        );

        taskListView.getItems().setAll(processed);
        if (processed.isEmpty()) {
            taskListView.setPlaceholder(new Label("Tidak ada tugas untuk filter ini."));
            detailContainer.setVisible(false);
        } else {
            taskListView.getSelectionModel().selectFirst();
        }
    }

    private enum SortChoice {
        NAME_ASC,
        NAME_DESC,
        DUE_DATE_ASC,
        DUE_DATE_DESC,
        DIFFICULTY_DESC
    }
}
