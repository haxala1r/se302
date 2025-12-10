package org.example.se302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.example.se302.service.DataManager;

/**
 * Main controller for the application window.
 * Manages the TabPane and status bar.
 */
public class MainController {

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab importTab;

    @FXML
    private Tab studentsTab;

    @FXML
    private Tab coursesTab;

    @FXML
    private Tab classroomsTab;

    @FXML
    private Tab scheduleTab;

    @FXML
    private Label statusLabel;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Initially disable data tabs until import is complete
        studentsTab.setDisable(true);
        coursesTab.setDisable(true);
        classroomsTab.setDisable(true);
        scheduleTab.setDisable(true);

        updateStatusBar();

        // Listen for data changes to automatically enable tabs
        dataManager.getStudents().addListener(
                (javafx.collections.ListChangeListener<org.example.se302.model.Student>) c -> {
                    if (dataManager.hasData()) {
                        enableDataTabs();
                    }
                });
    }

    /**
     * Enable data tabs after successful import.
     */
    public void enableDataTabs() {
        studentsTab.setDisable(false);
        coursesTab.setDisable(false);
        classroomsTab.setDisable(false);
        scheduleTab.setDisable(false);
        updateStatusBar();
    }

    /**
     * Update the status bar with current data counts.
     */
    public void updateStatusBar() {
        if (!dataManager.hasData()) {
            statusLabel.setText("Ready - No data loaded");
        } else {
            statusLabel.setText(String.format(
                    "Loaded: %d Students, %d Courses, %d Classrooms",
                    dataManager.getTotalStudents(),
                    dataManager.getTotalCourses(),
                    dataManager.getTotalClassrooms()
            ));
        }
    }
}
