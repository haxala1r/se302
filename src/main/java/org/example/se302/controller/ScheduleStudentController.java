package org.example.se302.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.se302.model.Student;
import org.example.se302.service.DataManager;

/**
 * Controller for the Student Schedule view.
 */
public class ScheduleStudentController {

    @FXML private ComboBox<Student> studentComboBox;
    @FXML private Label selectedStudentLabel;
    @FXML private TableView<CourseScheduleEntry> scheduleTable;
    @FXML private TableColumn<CourseScheduleEntry, String> courseColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> dateColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> timeColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> classroomColumn;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Populate student combo box
        studentComboBox.setItems(dataManager.getStudents());

        // Set up table columns
        courseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCourseCode()));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate()));
        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTime()));
        classroomColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClassroom()));
    }

    @FXML
    private void onShowSchedule() {
        Student selected = studentComboBox.getValue();
        if (selected == null) return;

        selectedStudentLabel.setText("Exam Schedule for: " + selected.getStudentId());

        // For demo: show enrolled courses with "Not Scheduled" status
        ObservableList<CourseScheduleEntry> entries = FXCollections.observableArrayList();
        for (String courseCode : selected.getEnrolledCourses()) {
            entries.add(new CourseScheduleEntry(courseCode, "Not Scheduled", "-", "-"));
        }

        scheduleTable.setItems(entries);
    }

    // Helper class for table entries
    public static class CourseScheduleEntry {
        private final String courseCode;
        private final String date;
        private final String time;
        private final String classroom;

        public CourseScheduleEntry(String courseCode, String date, String time, String classroom) {
            this.courseCode = courseCode;
            this.date = date;
            this.time = time;
            this.classroom = classroom;
        }

        public String getCourseCode() { return courseCode; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getClassroom() { return classroom; }
    }
}
