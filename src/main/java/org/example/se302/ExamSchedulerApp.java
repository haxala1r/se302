package org.example.se302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application for Exam Scheduling System.
 */
public class ExamSchedulerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                ExamSchedulerApp.class.getResource("view/main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);

        // Load CSS stylesheet
        String css = ExamSchedulerApp.class.getResource("css/application.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Exam Scheduling System v1.0");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
