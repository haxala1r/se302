package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.se302.model.Course;
import org.example.se302.service.DataManager;

/**
 * Controller for the Course Schedule view.
 */
public class ScheduleCourseController {

    @FXML private TableView<CourseScheduleEntry> courseScheduleTable;
    @FXML private TableColumn<CourseScheduleEntry, String> courseCodeColumn;
    @FXML private TableColumn<CourseScheduleEntry, Number> enrolledColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> dateColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> timeColumn;
    @FXML private TableColumn<CourseScheduleEntry, String> classroomColumn;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Set up table columns
        courseCodeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCourseCode()));
        enrolledColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getEnrolledCount()));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate()));
        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTime()));
        classroomColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClassroom()));

        // Load data
        loadScheduleData();

        // Listen for data changes
        dataManager.getCourses().addListener(
                (javafx.collections.ListChangeListener<Course>) c -> loadScheduleData());
    }

    private void loadScheduleData() {
        ObservableList<CourseScheduleEntry> entries = FXCollections.observableArrayList();

        for (Course course : dataManager.getCourses()) {
            entries.add(new CourseScheduleEntry(
                    course.getCourseCode(),
                    course.getEnrolledStudentsCount(),
                    "Not Scheduled",
                    "-",
                    "-"
            ));
        }

        courseScheduleTable.setItems(entries);
    }

    // Helper class for table entries
    public static class CourseScheduleEntry {
        private final String courseCode;
        private final int enrolledCount;
        private final String date;
        private final String time;
        private final String classroom;

        public CourseScheduleEntry(String courseCode, int enrolledCount, String date, String time, String classroom) {
            this.courseCode = courseCode;
            this.enrolledCount = enrolledCount;
            this.date = date;
            this.time = time;
            this.classroom = classroom;
        }

        public String getCourseCode() { return courseCode; }
        public int getEnrolledCount() { return enrolledCount; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getClassroom() { return classroom; }
    }
}
