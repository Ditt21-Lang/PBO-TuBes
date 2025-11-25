package com.pomodone.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainWindow.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Pomodone");

        // Set ikon aplikasi
        try (InputStream iconStream = getClass().getResourceAsStream("/images/icon.jpeg")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                System.err.println("Ikon aplikasi ga ketemu.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stage.setScene(scene);
        stage.show();
    }
}
