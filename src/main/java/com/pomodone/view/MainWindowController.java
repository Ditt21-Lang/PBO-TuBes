package com.pomodone.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Objects;

public class MainWindowController {

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

    @FXML
    public void initialize() {
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
            e.printStackTrace();
            Label errorLabel = new Label("Error: Gagal load halaman " + fxmlFile);
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
}
