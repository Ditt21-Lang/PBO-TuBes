package com.pomodone.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class MainWindowController {
    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

    @FXML
    private ScrollPane contentPane;

    @FXML
    private HBox homeNav;

    @FXML
    private HBox pomodoroNav;

    @FXML
    private HBox taskNav;

    @FXML
    private HBox settingsNav;

    private HBox activeNav;
    private static MainWindowController instance;

    @FXML
    public void initialize() {
        instance = this;
        // Set nav item yang aktif dan load tampilan awal
        activeNav = homeNav;
        navigateTo("DashboardView.fxml");

        homeNav.setOnMouseClicked(event -> {
            navigateTo("DashboardView.fxml");
            setActive(homeNav);
        });
        pomodoroNav.setOnMouseClicked(event -> {
            navigateTo("PomodoroView.fxml");
            setActive(pomodoroNav);
        });
        taskNav.setOnMouseClicked(event -> {
            navigateTo("TaskListView.fxml");
            setActive(taskNav);
        });
        settingsNav.setOnMouseClicked(event -> {
            navigateTo("SettingsView.fxml");
            setActive(settingsNav);
        });
    }

    private void navigateTo(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/" + fxmlFile)));
            contentPane.setContent(view);
        } catch (IOException | NullPointerException e) {
            log.error("Gagal memuat FXML {}", fxmlFile, e);
            Label errorLabel = new Label("Error: failed to load " + fxmlFile);
            contentPane.setContent(errorLabel);
        }
    }

    private void setActive(HBox navItem) {
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-item-active");
        }
        navItem.getStyleClass().add("nav-item-active");
        activeNav = navItem;
    }

    public static MainWindowController getInstance() {
        return instance;
    }

    public void navigateToTaskListWithSearch(String query) {
        com.pomodone.view.util.SearchContext.setPendingQuery(query);
        navigateTo("TaskListView.fxml");
        setActive(taskNav);
    }
}
