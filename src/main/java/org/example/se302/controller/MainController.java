package org.example.se302.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
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

    @FXML
    private Button helpButton;

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
            themeToggleButton.setText("🌙");
            isDarkMode = false;
        } else {
            // Switch to dark mode
            styleClass.add("dark");
            themeToggleButton.setText("🌞");
            isDarkMode = true;
        }
    }

    /**
     * Show the help dialog with comprehensive documentation.
     */
    @FXML
    private void onShowHelp() {
        Alert helpDialog = new Alert(Alert.AlertType.INFORMATION);
        helpDialog.setTitle("Help - Exam Scheduling System");
        helpDialog.setHeaderText("📅 Exam Scheduling System - User Guide");
        helpDialog.initModality(Modality.APPLICATION_MODAL);
        helpDialog.initOwner(contentArea.getScene().getWindow());

        String helpContent = """
                ════════════════════════════════════════════════════════════════════════
                                EXAM SCHEDULING SYSTEM - COMPLETE USER GUIDE
                ════════════════════════════════════════════════════════════════════════

                This guide walks you through every feature of the Exam Scheduling System,
                following the order you'll typically use them.


                ══════════════════════════════════════════════════════════════════════════
                🚀 STEP 1: GETTING STARTED
                ══════════════════════════════════════════════════════════════════════════

                When you first open the application, you'll see the main interface with:

                • SIDEBAR (Left): Navigation menu to access different sections
                • CONTENT AREA (Center): Displays the current view
                • STATUS BAR (Bottom): Shows loaded data counts
                • HEADER (Top Right): Help (❓) and Theme Toggle (🌙/🌞) buttons

                The sidebar buttons (Students, Courses, Classrooms, Schedule) are
                disabled until you import your data files.


                ══════════════════════════════════════════════════════════════════════════
                📁 STEP 2: IMPORTING YOUR DATA FILES
                ══════════════════════════════════════════════════════════════════════════

                The first step is to import your CSV data files. Click "Import Data"
                in the sidebar (it's already selected by default).

                You need to import 4 files in any order:

                1. STUDENT DATA (students.csv)
                   • Contains: Student ID, Student Name
                   • Format: id,name (one student per line)

                2. COURSE DATA (courses.csv)
                   • Contains: Course ID, Course Name, Duration (optional)
                   • Format: id,name,duration

                3. CLASSROOM DATA (classrooms.csv)
                   • Contains: Classroom ID, Classroom Name, Capacity
                   • Format: id,name,capacity

                4. ENROLLMENT DATA (enrollments.csv)
                   • Contains: Student ID, Course ID (links students to courses)
                   • Format: student_id,course_id

                HOW TO IMPORT:
                a) Click "Browse..." next to each file type
                b) Select the corresponding CSV file from your computer
                c) Repeat for all 4 files
                d) Click "📥 Import All" to load all data at once
                e) Check the "Import Messages" area for any errors or warnings

                Once imported successfully, the sidebar buttons will become enabled,
                and the status bar will show the count of loaded items.


                ══════════════════════════════════════════════════════════════════════════
                📅 STEP 3: GENERATING A SCHEDULE
                ══════════════════════════════════════════════════════════════════════════

                Click "Schedule" in the sidebar to open the Auto Schedule Generator.

                CONFIGURATION OPTIONS:

                • Number of Days: How many exam days (e.g., 5-10 days)
                • Slots per Day: Time slots each day (e.g., 3-5 slots)
                • Start Date: When the exam period begins
                • Slot Duration: Minutes per exam slot (e.g., 60, 90, 120)
                • Day Start Time: When the first exam starts (e.g., 09:00)
                • Optimization Strategy: Algorithm approach for scheduling
                • Allow Back-to-Back: Whether students can have consecutive exams

                GENERATING THE SCHEDULE:
                a) Adjust the configuration settings as needed
                b) Click "🚀 Generate Schedule"
                c) Wait for the algorithm to complete (progress shown)
                d) View the generated schedule in the grid below

                The schedule grid shows:
                • Rows = Time slots
                • Columns = Days
                • Each cell = Exam assignment with course and classroom

                EDITING THE SCHEDULE:
                • DRAG & DROP: Click and drag any exam to a different time slot
                • EDIT DETAILS: Click on an exam to open the edit dialog
                • The system will warn you about conflicts (student overlap, etc.)

                EXPORTING:
                • Click "📥 Export CSV" to save the schedule as a spreadsheet


                ══════════════════════════════════════════════════════════════════════════
                👤 STEP 4: VIEWING STUDENT INFORMATION
                ══════════════════════════════════════════════════════════════════════════

                Click "Students" in the sidebar - it expands to show two options:

                STUDENT LIST:
                • View all imported students in a table
                • See each student's ID and name
                • Search or filter students (if available)

                STUDENT SCHEDULE:
                • Select a specific student to view their personal exam timetable
                • See which exams they're enrolled in
                • View dates, times, and classroom locations


                ══════════════════════════════════════════════════════════════════════════
                📚 STEP 5: VIEWING COURSE INFORMATION
                ══════════════════════════════════════════════════════════════════════════

                Click "Courses" in the sidebar - it expands to show two options:

                COURSE LIST:
                • View all courses with details
                • See enrollment counts (how many students per course)
                • View assigned classrooms and times

                COURSE SCHEDULE:
                • View exam schedule organized by course
                • See when and where each course exam takes place
                • Useful for faculty and exam coordinators


                ══════════════════════════════════════════════════════════════════════════
                🏛️ STEP 6: VIEWING CLASSROOM INFORMATION
                ══════════════════════════════════════════════════════════════════════════

                Click "Classrooms" in the sidebar - it expands to show two options:

                CLASSROOM LIST:
                • View all classrooms with their capacities
                • See which classrooms can fit which courses
                • Useful for room planning

                CLASSROOM SCHEDULE:
                • View exam schedule organized by room
                • See what exams are in each classroom
                • Useful for room coordinators and proctors


                ══════════════════════════════════════════════════════════════════════════
                🌙 THEME TOGGLE
                ══════════════════════════════════════════════════════════════════════════

                Click the moon (🌙) or sun (🌞) icon in the top-right corner to switch
                between Light Mode and Dark Mode for your viewing preference.


                ══════════════════════════════════════════════════════════════════════════
                💡 TIPS & BEST PRACTICES
                ══════════════════════════════════════════════════════════════════════════

                1. PREPARE YOUR DATA: Make sure your CSV files are correctly formatted
                   before importing. Check column headers match expected format.

                2. START WITH DEFAULTS: Use default schedule settings first, then
                   adjust based on results.

                3. REVIEW CONFLICTS: After generating, check for any warnings about
                   student conflicts or capacity issues.

                4. SAVE YOUR WORK: Export your schedule to CSV regularly so you
                   don't lose your progress.

                5. USE STUDENT VIEW: To verify no single student has exam conflicts,
                   check individual student schedules.

                6. CLASSROOM CAPACITY: Ensure classrooms can fit all enrolled students
                   by checking the Classroom List view.


                ══════════════════════════════════════════════════════════════════════════
                ❓ NEED MORE HELP?
                ══════════════════════════════════════════════════════════════════════════

                If you encounter issues:
                • Check the Import Messages for data errors
                • Verify your CSV files are properly formatted
                • Try with fewer constraints if schedule generation fails
                • Increase number of days or slots if courses don't fit
                """;

        // Create a scrollable TextArea for the help content
        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(helpContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13px;");

        // Set the TextArea as expandable content
        helpDialog.getDialogPane().setExpandableContent(null);
        helpDialog.getDialogPane().setContent(textArea);

        // Make dialog larger with fixed size
        helpDialog.getDialogPane().setMinWidth(750);
        helpDialog.getDialogPane().setMinHeight(600);
        helpDialog.getDialogPane().setPrefWidth(750);
        helpDialog.getDialogPane().setPrefHeight(600);
        helpDialog.setResizable(false);

        helpDialog.showAndWait();
    }
}
