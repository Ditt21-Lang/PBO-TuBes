package com.pomodone.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pomodone.facade.TaskManagementFacade;
import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.service.TaskService;
import com.pomodone.strategy.task.DifficultyAscSortStrategy;
import com.pomodone.strategy.task.DifficultyDescSortStrategy;
import com.pomodone.strategy.task.DueDateAscSortStrategy;
import com.pomodone.strategy.task.DueDateDescSortStrategy;
import com.pomodone.strategy.task.NameAscSortStrategy;
import com.pomodone.strategy.task.NameDescSortStrategy;
import com.pomodone.strategy.task.TaskSortStrategy;
import com.pomodone.util.CollectionViewProcessor;
import com.pomodone.util.SortDirection;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TaskListController {
    private static final Logger log = LoggerFactory.getLogger(TaskListController.class);

    @FXML private ListView<Task> taskListView;
    @FXML private Button addTaskButton;
    @FXML private Button viewSettingsButton;
    @FXML private Button closeFilterPanelButton;
    @FXML private VBox filterPanel;
    @FXML private TextField taskSearchField;
    @FXML private CheckBox pendingCheckBox;
    @FXML private CheckBox overdueCheckBox;
    @FXML private CheckBox doneCheckBox;
    @FXML private RadioButton sortNameAscRadio;
    @FXML private RadioButton sortNameDescRadio;
    @FXML private RadioButton sortDueSoonRadio;
    @FXML private RadioButton sortDueLateRadio;
    @FXML private RadioButton sortDifficultyAscRadio;
    @FXML private RadioButton sortDifficultyDescRadio;
    
    private static final String STYLE_PRIORITY_HIGH = "task-item-priority-high";
    private static final String STYLE_PRIORITY_MEDIUM = "task-item-priority-medium";
    private static final String STYLE_PRIORITY_LOW = "task-item-priority-low";
    
    @FXML private VBox detailContainer;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailDeadlineLabel;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Button editTaskButton;
    @FXML private Button deleteTaskButton;
    @FXML private Button markDoneButton;


    private TaskService taskService;
    private final CollectionViewProcessor<Task> viewProcessor = new CollectionViewProcessor<>();
    private final DateTimeFormatter deadlineFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
    private final DateTimeFormatter listDeadlineFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"));
    private List<Task> allTasks = new ArrayList<>();
    private String currentSearch = "";
    private final Map<SortChoice, TaskSortStrategy> sortStrategies = new EnumMap<>(SortChoice.class);
    private Task selectedTask;
    private final TaskManagementFacade taskFacade = new TaskManagementFacade();


    @FXML
    public void initialize() {
        this.taskService = new TaskService();

        setupListViewCellFactory();
        setupListListener();
        setupAddButton();
        setupFilterControls();
        setupSortControls();
        setupSortStrategies();
        setupSearchField();
        loadTaskFromDatabase();
        applyPendingSearch();
        applyPendingAdd();
        deleteTaskButton.setOnAction(e -> handleDeleteTask());
        editTaskButton.setOnAction(e -> showEditTaskDialog(selectedTask));
        markDoneButton.setOnAction(e -> handleMarkDone());
    }

    private void setupAddButton() {
        addTaskButton.setOnAction(event -> showAddTaskDialog());
    }

    private void setupSearchField() {
        if (taskSearchField == null) return;
        taskSearchField.textProperty().addListener((obs, oldV, newV) -> {
            currentSearch = newV != null ? newV.trim() : "";
            refreshTaskList();
        });
    }

    private void applyPendingSearch() {
        String pending = com.pomodone.view.util.SearchContext.consumePendingQuery();
        if (pending != null && taskSearchField != null) {
            taskSearchField.setText(pending);
            currentSearch = pending.trim();
            refreshTaskList();
        }
    }

    private void applyPendingAdd() {
        boolean shouldAdd = com.pomodone.view.util.SearchContext.consumePendingAdd();
        if (shouldAdd) {
            showAddTaskDialog();
        }
    }

    private void setupListListener() {
        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newTask) -> {
            selectedTask = newTask;
            if (newTask != null) {
                showTaskDetail(newTask);
            } else {
                detailContainer.setVisible(false);
                markDoneButton.setDisable(true);
            }
        });
    }


    private void setupListViewCellFactory() {
        taskListView.setCellFactory(listView -> new DashboardTaskCell());
    }

    private class DashboardTaskCell extends ListCell<Task> {
        private final Label titleLabel = new Label();
        private final Label difficultyLabel = new Label();
        private final Label dueDateLabel = new Label();
        private final Region spacer = new Region();
        private final HBox card;

        DashboardTaskCell() {
            titleLabel.getStyleClass().add("task-item-title");
            difficultyLabel.getStyleClass().add(STYLE_PRIORITY_LOW);
            dueDateLabel.getStyleClass().add("task-item-due-date");

            VBox infoBox = new VBox(titleLabel, difficultyLabel);
            infoBox.setSpacing(4);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            card = new HBox(infoBox, spacer, dueDateLabel);
            card.setSpacing(8);
            card.setAlignment(Pos.BOTTOM_LEFT);
            card.setPadding(new Insets(12));
            card.getStyleClass().add("task-item");
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            titleLabel.setText(truncateTitle(task.getTitle(), 38));
            difficultyLabel.setText(formatDifficultyLabel(task.getDifficulty()));
            updateDifficultyStyle(task.getDifficulty());
            dueDateLabel.setText(formatDueDate(task.getDueDate()));
            updateDueDateStyle(task);

            setText(null);
            setGraphic(card);
        }

        private void updateDifficultyStyle(TaskDifficulty difficulty) {
            difficultyLabel.getStyleClass().removeAll(
                    STYLE_PRIORITY_HIGH,
                    STYLE_PRIORITY_MEDIUM,
                    STYLE_PRIORITY_LOW
            );
            difficultyLabel.getStyleClass().add(resolveDifficultyStyle(difficulty));
        }

        private String formatDifficultyLabel(TaskDifficulty difficulty) {
            return switch (difficulty) {
                case SULIT -> "Sulit";
                case SEDANG -> "Sedang";
                case MUDAH -> "Mudah";
            };
        }

        private String resolveDifficultyStyle(TaskDifficulty difficulty) {
            return switch (difficulty) {
                case SULIT -> STYLE_PRIORITY_HIGH;
                case SEDANG -> STYLE_PRIORITY_MEDIUM;
                case MUDAH -> STYLE_PRIORITY_LOW;
            };
        }

        private String formatDueDate(LocalDateTime dueDate) {
            if (dueDate == null) return "";
            return "Due: " + dueDate.format(listDeadlineFormatter);
        }

        private String truncateTitle(String title, int maxChars) {
            if (title == null) return "";
            if (title.length() <= maxChars) return title;
            return title.substring(0, maxChars - 3) + "...";
        }

        private void updateDueDateStyle(Task task) {
            // reset to base style
            dueDateLabel.setStyle("");
            if (task.getStatus() == TaskStatus.TERLAMBAT) {
                dueDateLabel.setStyle("-fx-text-fill: #DC2626;"); // red-600
            }
        }
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
        ToggleGroup sortToggleGroup = new ToggleGroup();
        sortNameAscRadio.setToggleGroup(sortToggleGroup);
        sortNameDescRadio.setToggleGroup(sortToggleGroup);
        sortDueSoonRadio.setToggleGroup(sortToggleGroup);
        sortDueLateRadio.setToggleGroup(sortToggleGroup);
        sortDifficultyAscRadio.setToggleGroup(sortToggleGroup);
        sortDifficultyDescRadio.setToggleGroup(sortToggleGroup);

        // default: due date terdekat lebih dulu
        sortDueSoonRadio.setSelected(true);
        sortToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> refreshTaskList());
    }

    private void setupSortStrategies() {
        sortStrategies.put(SortChoice.NAME_ASC, new NameAscSortStrategy());
        sortStrategies.put(SortChoice.NAME_DESC, new NameDescSortStrategy());
        sortStrategies.put(SortChoice.DUE_DATE_ASC, new DueDateAscSortStrategy());
        sortStrategies.put(SortChoice.DUE_DATE_DESC, new DueDateDescSortStrategy());
        sortStrategies.put(SortChoice.DIFFICULTY_ASC, new DifficultyAscSortStrategy());
        sortStrategies.put(SortChoice.DIFFICULTY_DESC, new DifficultyDescSortStrategy());
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
        if (sortDifficultyAscRadio.isSelected()) return SortChoice.DIFFICULTY_ASC;
        if (sortDifficultyDescRadio.isSelected()) return SortChoice.DIFFICULTY_DESC;
        // default fallback
        return SortChoice.DUE_DATE_ASC;
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
        datePicker.setPromptText("dd/MM/yyyy");

        Spinner<Integer> hourSpinner = new Spinner<>(new IntegerSpinnerValueFactory(0, 23, 23));
        Spinner<Integer> minuteSpinner = new Spinner<>(new IntegerSpinnerValueFactory(0, 59, 59));
        hourSpinner.setEditable(false);
        minuteSpinner.setEditable(false);
        hourSpinner.disableProperty().bind(datePicker.valueProperty().isNull());
        minuteSpinner.disableProperty().bind(datePicker.valueProperty().isNull());

        ComboBox<TaskDifficulty> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().setAll(TaskDifficulty.values());
        difficultyBox.setValue(TaskDifficulty.SEDANG);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Deadline:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Time:"), 0, 3);
        HBox timeBox = new HBox(6, hourSpinner, new Label(":"), minuteSpinner);
        grid.add(timeBox, 1, 3);
        grid.add(new Label("Difficulty:"), 0, 4);
        grid.add(difficultyBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText();
                String desc = descField.getText();
                TaskDifficulty diff = difficultyBox.getValue();
                
                LocalDateTime dueDate = null;
                LocalDate selectedDate = datePicker.getValue();
                if (selectedDate != null) {
                    LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                    dueDate = LocalDateTime.of(selectedDate, time);
                }

                try {
                    taskService.createNewTask(title, desc, dueDate, diff);
                    return null;
                } catch (Exception e) {
                    log.error("Gagal menyimpan task baru", e);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save: " + e.getMessage());
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
            markDoneButton.setDisable(true);
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
        markDoneButton.setDisable(false);
        updateMarkButtonLabel(task);
    }

    private void loadTaskFromDatabase() {
        try {
            allTasks = taskService.getAllTasks();
            refreshTaskList();
        } catch (Exception e) {
            log.error("Gagal memuat task dari database", e);
        }
    }

    private boolean statusSelected(Task task) {
        boolean statusOk = (pendingCheckBox.isSelected() && task.getStatus() == TaskStatus.BELUM_SELESAI)
                || (overdueCheckBox.isSelected() && task.getStatus() == TaskStatus.TERLAMBAT)
                || (doneCheckBox.isSelected() && task.getStatus() == TaskStatus.SELESAI);
        return statusOk && matchesSearch(task);
    }

    private boolean matchesSearch(Task task) {
        if (currentSearch == null || currentSearch.isEmpty()) return true;
        String query = currentSearch.toLowerCase();
        return (task.getTitle() != null && task.getTitle().toLowerCase().contains(query))
                || (task.getDescription() != null && task.getDescription().toLowerCase().contains(query));
    }

    private void refreshTaskList() {
        SortChoice sortChoice = resolveSortChoice();
        TaskSortStrategy strategy = sortStrategies.getOrDefault(sortChoice, sortStrategies.get(SortChoice.DUE_DATE_ASC));
        Comparator<Task> comparator = strategy != null ? strategy.getComparator() : null;
        SortDirection direction = strategy != null ? strategy.getDirection() : SortDirection.ASC;

        List<Task> processed = viewProcessor.apply(
                allTasks,
                this::statusSelected,
                comparator,
                direction
        );

        taskListView.getItems().setAll(processed);
        if (processed.isEmpty()) {
            taskListView.setPlaceholder(new Label("No tasks for this filter/search."));
            detailContainer.setVisible(false);
        } else {
            taskListView.getSelectionModel().selectFirst();
        }
    }

    private void handleDeleteTask() {
        if (selectedTask == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Are you sure you want to delete this task?");
        confirm.setContentText("Task: " + selectedTask.getTitle());

        ButtonType deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        confirm.getButtonTypes().setAll(deleteButton, cancelButton);

        confirm.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                // Kalau user menekan Delete -> baru eksekusi
                taskFacade.destroyTask((int) selectedTask.getId());
                loadTaskFromDatabase();
                detailContainer.setVisible(false);
            }
        });
    }


    private void handleMarkDone() {
        if (selectedTask == null) return;

        TaskStatus targetStatus = selectedTask.getStatus() == TaskStatus.SELESAI
                ? resolveUndoStatus(selectedTask)
                : TaskStatus.SELESAI;

        taskFacade.saveTask(
                selectedTask.getId(),
                selectedTask.getTitle(),
                selectedTask.getDescription(),
                selectedTask.getDueDate(),
                selectedTask.getDifficulty(),
                targetStatus
        );

        loadTaskFromDatabase();
        detailContainer.setVisible(false);
    }

    private TaskStatus resolveUndoStatus(Task task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getDueDate() == null || task.getDueDate().isAfter(now)) {
            return TaskStatus.BELUM_SELESAI;
        }
        return TaskStatus.TERLAMBAT;
    }

    private void updateMarkButtonLabel(Task task) {
        if (task.getStatus() == TaskStatus.SELESAI) {
            markDoneButton.setText("Mark as not done");
        } else {
            markDoneButton.setText("Mark as done");
        }
    }

    private void showEditTaskDialog(Task task) {
        if (task == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Modify your task details");

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Fields
        TextField titleField = new TextField(task.getTitle());
        TextArea descField = new TextArea(task.getDescription());
        descField.setPrefHeight(100);

        LocalDateTime due = task.getDueDate();

        DatePicker datePicker = new DatePicker(
                due != null ? due.toLocalDate() : null
        );

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23,
                due != null ? due.getHour() : 12
        );
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59,
                due != null ? due.getMinute() : 0
        );

        // Disable time if no date
        boolean hasDate = (due != null);
        hourSpinner.setDisable(!hasDate);
        minuteSpinner.setDisable(!hasDate);

        // Auto-disable time if date cleared
        datePicker.valueProperty().addListener((obs, oldV, newV) -> {
            boolean enabled = newV != null;

            hourSpinner.setDisable(!enabled);
            minuteSpinner.setDisable(!enabled);

            if (!enabled) {
                // Reset spinner when no date
                hourSpinner.getValueFactory().setValue(12);
                minuteSpinner.getValueFactory().setValue(0);
            }
        });

        ComboBox<TaskDifficulty> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().setAll(TaskDifficulty.values());
        difficultyBox.setValue(task.getDifficulty());

        // Layout
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);

        grid.add(new Label("Deadline:"), 0, 2);
        grid.add(datePicker, 1, 2);

        HBox timeBox = new HBox(6, hourSpinner, new Label(":"), minuteSpinner);
        grid.add(new Label("Time:"), 0, 3);
        grid.add(timeBox, 1, 3);

        grid.add(new Label("Difficulty:"), 0, 4);
        grid.add(difficultyBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {

                LocalDate date = datePicker.getValue();
                LocalDateTime dueDate = null;

                if (date != null) {
                    LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                    dueDate = LocalDateTime.of(date, time);
                }

                taskFacade.saveTask(
                        task.getId(),
                        titleField.getText(),
                        descField.getText(),
                        dueDate,
                        difficultyBox.getValue(),
                        task.getStatus()
                );

                loadTaskFromDatabase();
            }
            return null;
        });

        dialog.showAndWait();
    }




    private enum SortChoice {
        NAME_ASC,
        NAME_DESC,
        DUE_DATE_ASC,
        DUE_DATE_DESC,
        DIFFICULTY_ASC,
        DIFFICULTY_DESC
    }
}
