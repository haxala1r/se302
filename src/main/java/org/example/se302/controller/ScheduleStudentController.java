package org.example.se302.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.se302.model.Course;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.model.Student;
import org.example.se302.model.TimeSlot;
import org.example.se302.service.DataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for the Student Schedule view.
 * Displays exam schedule for a selected student.
 */
public class ScheduleStudentController {

    @FXML
    private ComboBox<Student> studentComboBox;
    @FXML
    private Label selectedStudentLabel;
    @FXML
    private TableView<CourseScheduleEntry> scheduleTable;
    @FXML
    private TableColumn<CourseScheduleEntry, String> courseColumn;
    @FXML
    private TableColumn<CourseScheduleEntry, String> dateColumn;
    @FXML
    private TableColumn<CourseScheduleEntry, String> timeColumn;
    @FXML
    private TableColumn<CourseScheduleEntry, String> classroomColumn;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Populate student combo box
        studentComboBox.setItems(dataManager.getStudents());

        // Enable filtering/search in combobox ideally, but for now simple selection
        studentComboBox.setPromptText("Select a student...");

        // Set up table columns
        courseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCourseCode()));
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateDisplay()));
        timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeDisplay()));
        classroomColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClassroom()));

        // Custom row factory for highlighting
        scheduleTable.setRowFactory(tv -> new TableRow<CourseScheduleEntry>() {
            @Override
            protected void updateItem(CourseScheduleEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    setTooltip(null);
                } else {
                    // Logic to highlight rows
                    boolean styleSet = false;

                    if (item.hasConflictWarning) {
                        setStyle("-fx-background-color: #ffcdd2;"); // Red tint (conflict)
                        setTooltip(new Tooltip("Conflict: Multiple exams at same time!"));
                        styleSet = true;
                    } else if (item.isMultipleExamsOnDay) {
                        setStyle("-fx-background-color: #fff3cd;"); // Yellow tint
                        setTooltip(new Tooltip("Warning: Multiple exams on this day"));
                        styleSet = true;
                    } else if (item.isConsecutiveDay) {
                        setStyle("-fx-background-color: #e3f2fd;"); // Blue tint
                        setTooltip(new Tooltip("Info: Exam on consecutive day"));
                        styleSet = true;
                    }

                    if (!styleSet) {
                        setStyle("");
                        setTooltip(null);
                    }
                }
            }
        });
    }

    @FXML
    private void onShowSchedule() {
        Student selected = studentComboBox.getValue();
        if (selected == null)
            return;

        selectedStudentLabel.setText("Exam Schedule for: " + selected.getStudentId());

        refreshTable(selected);
    }

    private void refreshTable(Student student) {
        List<CourseScheduleEntry> entries = new ArrayList<>();
        ScheduleConfiguration config = dataManager.getActiveConfiguration();

        for (String courseCode : student.getEnrolledCourses()) {
            Course course = dataManager.getCourse(courseCode);
            if (course == null)
                continue;

            String dateStr = "Not Scheduled";
            String timeStr = "-";
            String classroom = "-";
            int dayIndex = -1, slotIndex = -1;

            if (course.isScheduled()) {
                dayIndex = course.getExamDay();
                slotIndex = course.getExamTimeSlot();
                classroom = course.getAssignedClassroom();

                TimeSlot slot = config != null ? config.getTimeSlot(dayIndex, slotIndex) : null;
                if (slot != null) {
                    dateStr = slot.getDate().toString();
                    timeStr = slot.getStartTime() + " - " + slot.getEndTime();
                } else {
                    dateStr = "Day " + (dayIndex + 1);
                    timeStr = "Slot " + (slotIndex + 1);
                }
            }

            entries.add(new CourseScheduleEntry(courseCode, dateStr, timeStr, classroom, dayIndex, slotIndex));
        }

        entries.sort(Comparator.comparingInt(CourseScheduleEntry::getDayIndex)
                .thenComparingInt(CourseScheduleEntry::getSlotIndex));
        analyzeSchedule(entries);
        scheduleTable.setItems(FXCollections.observableArrayList(entries));
    }

    private void analyzeSchedule(List<CourseScheduleEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            CourseScheduleEntry current = entries.get(i);
            if (current.getDayIndex() == -1)
                continue;

            // Check for multiple exams and conflicts on same day
            for (CourseScheduleEntry other : entries) {
                if (other.getDayIndex() == current.getDayIndex() && other.getDayIndex() != -1) {
                    current.isMultipleExamsOnDay = true;
                    if (other.getSlotIndex() == current.getSlotIndex() && other != current) {
                        current.hasConflictWarning = true;
                    }
                }
            }

            // Check for consecutive days with previous exam
            if (i > 0) {
                CourseScheduleEntry prev = entries.get(i - 1);
                if (prev.getDayIndex() != -1 && current.getDayIndex() == prev.getDayIndex() + 1) {
                    current.isConsecutiveDay = true;
                }
            }
        }
    }

    // Helper class for table entries
    public static class CourseScheduleEntry {
        private final String courseCode;
        private final String dateDisplay;
        private final String timeDisplay;
        private final String classroom;
        private final int dayIndex;
        private final int slotIndex;

        // View flags
        public boolean isMultipleExamsOnDay = false;
        public boolean isConsecutiveDay = false;
        public boolean hasConflictWarning = false;

        public CourseScheduleEntry(String courseCode, String dateDisplay, String timeDisplay, String classroom,
                int dayIndex, int slotIndex) {
            this.courseCode = courseCode;
            this.dateDisplay = dateDisplay;
            this.timeDisplay = timeDisplay;
            this.classroom = classroom;
            this.dayIndex = dayIndex;
            this.slotIndex = slotIndex;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public String getDateDisplay() {
            return dateDisplay;
        }

        public String getTimeDisplay() {
            return timeDisplay;
        }

        public String getClassroom() {
            return classroom;
        }

        public int getDayIndex() {
            return dayIndex;
        }

        public int getSlotIndex() {
            return slotIndex;
        }
    }

    /**
     * Exports the selected student's schedule as CSV.
     */
    @FXML
    private void onExportCSV() {
        Student selected = studentComboBox.getValue();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Student",
                    "Please select a student first.");
            return;
        }

        if (scheduleTable.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data",
                    "No schedule data to export.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Student Schedule as CSV");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("schedule_student_" + selected.getStudentId() + ".csv");

        java.io.File file = fileChooser.showSaveDialog(scheduleTable.getScene().getWindow());
        if (file == null)
            return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            // Header
            writer.println("Student ID,Course,Date,Time,Classroom");

            // Data rows
            for (CourseScheduleEntry entry : scheduleTable.getItems()) {
                writer.println(String.format("%s,%s,\"%s\",%s,%s",
                        selected.getStudentId(),
                        entry.getCourseCode(),
                        entry.getDateDisplay(),
                        entry.getTimeDisplay(),
                        entry.getClassroom()));
            }

            showAlert(Alert.AlertType.INFORMATION, "Export Complete",
                    "Student schedule exported to:\n" + file.getAbsolutePath());
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not write file: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
