package org.example.se302.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import org.example.se302.service.DataManager;

import java.io.IOException;

/**
 * Main controller for the application window.
 * Manages the sidebar navigation, content area, status bar, and theme toggling.
 */
public class MainController {

    @FXML
    private VBox sidebar;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button importBtn;

    @FXML
    private Button studentsBtn;

    @FXML
    private Button coursesBtn;

    @FXML
    private Button classroomsBtn;

    @FXML
    private Button scheduleBtn;

    // Sub-menus
    @FXML
    private VBox studentsSubMenu;

    @FXML
    private VBox coursesSubMenu;

    @FXML
    private VBox classroomsSubMenu;

    // Sub-menu buttons
    @FXML
    private Button studentListBtn;

    @FXML
    private Button studentScheduleBtn;

    @FXML
    private Button courseListBtn;

    @FXML
    private Button courseScheduleBtn;

    @FXML
    private Button classroomListBtn;

    @FXML
    private Button classroomScheduleBtn;

    @FXML
    private Label statusLabel;

    @FXML
    private Button themeToggleButton;

    private DataManager dataManager;
    private boolean isDarkMode = false;

    // Track which sub-menus are open
    private boolean studentsMenuOpen = false;
    private boolean coursesMenuOpen = false;
    private boolean classroomsMenuOpen = false;

    // Cached views
    private Node importView;
    private Node studentsView;
    private Node coursesView;
    private Node classroomsView;
    private Node calendarView;
    private Node studentScheduleView;
    private Node courseScheduleView;
    private Node classroomScheduleView;

    private Button activeButton;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Pre-load main views
        importView = loadView("import-view.fxml");
        studentsView = loadView("students-view.fxml");
        coursesView = loadView("courses-view.fxml");
        classroomsView = loadView("classrooms-view.fxml");

        // Pre-load schedule views to eliminate delay
        calendarView = loadView("schedule-calendar-view.fxml");
        studentScheduleView = loadView("schedule-student-view.fxml");
        courseScheduleView = loadView("schedule-course-view.fxml");
        classroomScheduleView = loadView("schedule-classroom-view.fxml");

        // Show import view initially
        contentArea.getChildren().clear();
        contentArea.getChildren().add(importView);

        // Set import as active by default
        setActiveButton(importBtn);

        updateStatusBar();

        // Listen for data changes to automatically enable buttons
        dataManager.getStudents().addListener(
                (javafx.collections.ListChangeListener<org.example.se302.model.Student>) c -> {
                    if (dataManager.hasData()) {
                        enableDataButtons();
                    }
                });
    }

    /**
     * Enable data buttons after successful import.
     */
    public void enableDataButtons() {
        studentsBtn.setDisable(false);
        coursesBtn.setDisable(false);
        classroomsBtn.setDisable(false);
        scheduleBtn.setDisable(false);
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
                    dataManager.getTotalClassrooms()));
        }
    }

    /**
     * Set the active button style
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("sidebar-button-active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("sidebar-button-active");
    }

    /**
     * Show a view in the content area
     */
    private void showView(Node view, Button button) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
        setActiveButton(button);
    }

    /**
     * Load a view from FXML
     */
    private Node loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/se302/view/" + fxmlPath));
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading view: " + fxmlPath);
        }
    }

    @FXML
    private void onShowImport() {
        showView(importView, importBtn);
    }

    /**
     * Toggle students submenu visibility
     */
    @FXML
    private void onToggleStudentsMenu() {
        if (!studentsMenuOpen) {
            studentsMenuOpen = true;
            studentsSubMenu.setVisible(true);
            studentsSubMenu.setManaged(true);
            // Show student list by default when opening
            showView(studentsView, studentListBtn);
        } else {
            studentsMenuOpen = false;
            studentsSubMenu.setVisible(false);
            studentsSubMenu.setManaged(false);
        }
    }

    @FXML
    private void onShowStudents() {
        showView(studentsView, studentListBtn);
    }

    /**
     * Toggle courses submenu visibility
     */
    @FXML
    private void onToggleCoursesMenu() {
        if (!coursesMenuOpen) {
            coursesMenuOpen = true;
            coursesSubMenu.setVisible(true);
            coursesSubMenu.setManaged(true);
            // Show course list by default when opening
            showView(coursesView, courseListBtn);
        } else {
            coursesMenuOpen = false;
            coursesSubMenu.setVisible(false);
            coursesSubMenu.setManaged(false);
        }
    }

    @FXML
    private void onShowCourses() {
        showView(coursesView, courseListBtn);
    }

    /**
     * Toggle classrooms submenu visibility
     */
    @FXML
    private void onToggleClassroomsMenu() {
        if (!classroomsMenuOpen) {
            classroomsMenuOpen = true;
            classroomsSubMenu.setVisible(true);
            classroomsSubMenu.setManaged(true);
            // Show classroom list by default when opening
            showView(classroomsView, classroomListBtn);
        } else {
            classroomsMenuOpen = false;
            classroomsSubMenu.setVisible(false);
            classroomsSubMenu.setManaged(false);
        }
    }

    @FXML
    private void onShowClassrooms() {
        showView(classroomsView, classroomListBtn);
    }

    /**
     * Show Calendar View directly when Schedule is clicked
     */
    @FXML
    private void onShowCalendar() {
        showView(calendarView, scheduleBtn);
    }

    @FXML
    private void onShowStudentSchedule() {
        showView(studentScheduleView, studentScheduleBtn);
    }

    @FXML
    private void onShowCourseSchedule() {
        showView(courseScheduleView, courseScheduleBtn);
    }

    @FXML
    private void onShowClassroomSchedule() {
        showView(classroomScheduleView, classroomScheduleBtn);
    }

    /**
     * Toggle between light and dark themes.
     */
    @FXML
    private void onToggleTheme() {
        var root = contentArea.getScene().getRoot();
        var styleClass = root.getStyleClass();

        if (isDarkMode) {
            // Switch to light mode
            styleClass.remove("dark");
            themeToggleButton.setText("🌙 Dark Mode");
            isDarkMode = false;
        } else {
            // Switch to dark mode
            styleClass.add("dark");
            themeToggleButton.setText("☀️ Light Mode");
            isDarkMode = true;
        }
    }
}
