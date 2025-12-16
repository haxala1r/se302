package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.se302.model.Course;
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.service.DataManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for the Course Schedule view.
 * Displays courses with their exam date, time, and assigned classroom.
 */
public class ScheduleCourseController {

        @FXML
        private TableView<CourseScheduleEntry> courseScheduleTable;
        @FXML
        private TableColumn<CourseScheduleEntry, String> courseCodeColumn;
        @FXML
        private TableColumn<CourseScheduleEntry, Number> enrolledColumn;
        @FXML
        private TableColumn<CourseScheduleEntry, String> dateColumn;
        @FXML
        private TableColumn<CourseScheduleEntry, String> timeColumn;
        @FXML
        private TableColumn<CourseScheduleEntry, String> classroomColumn;

        private DataManager dataManager;

        // Reference to the current schedule state (set by parent controller or loaded
        // from DB)
        private Map<String, ExamAssignment> currentAssignments;
        private ScheduleConfiguration configuration;

        @FXML
        public void initialize() {
                dataManager = DataManager.getInstance();

                // Set up table columns
                courseCodeColumn.setCellValueFactory(
                                cellData -> new SimpleStringProperty(cellData.getValue().getCourseCode()));
                enrolledColumn.setCellValueFactory(
                                cellData -> new SimpleIntegerProperty(cellData.getValue().getEnrolledCount()));
                dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
                timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
                classroomColumn.setCellValueFactory(
                                cellData -> new SimpleStringProperty(cellData.getValue().getClassroom()));

                // Load data
                loadScheduleData();

                // Listen for data changes
                dataManager.getCourses().addListener(
                                (javafx.collections.ListChangeListener<Course>) c -> loadScheduleData());
        }

        /**
         * Sets the current schedule assignments. Called by parent controller after
         * schedule generation.
         */
        public void setScheduleData(Map<String, ExamAssignment> assignments, ScheduleConfiguration config) {
                this.currentAssignments = assignments;
                this.configuration = config;
                loadScheduleData();
        }

        private void loadScheduleData() {
                ObservableList<CourseScheduleEntry> entries = FXCollections.observableArrayList();

                for (Course course : dataManager.getCourses()) {
                        String courseCode = course.getCourseCode();
                        int enrolled = course.getEnrolledStudentsCount();

                        String dateStr = "Not Scheduled";
                        String timeStr = "-";
                        String classroomStr = "-";

                        // Check if we have an assignment for this course
                        if (currentAssignments != null && currentAssignments.containsKey(courseCode)) {
                                ExamAssignment assignment = currentAssignments.get(courseCode);

                                if (assignment.isAssigned()) {
                                        // Format date based on configuration start date + day offset
                                        if (configuration != null && configuration.getStartDate() != null) {
                                                LocalDate examDate = configuration.getStartDate()
                                                                .plusDays(assignment.getDay());
                                                dateStr = examDate.format(
                                                                DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)"));
                                        } else {
                                                dateStr = "Day " + (assignment.getDay() + 1);
                                        }

                                        // Format time slot
                                        timeStr = "Slot " + (assignment.getTimeSlotIndex() + 1);

                                        // Classroom
                                        classroomStr = assignment.getClassroomId() != null ? assignment.getClassroomId()
                                                        : "-";
                                }
                        }

                        entries.add(new CourseScheduleEntry(courseCode, enrolled, dateStr, timeStr, classroomStr));
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

                public CourseScheduleEntry(String courseCode, int enrolledCount, String date, String time,
                                String classroom) {
                        this.courseCode = courseCode;
                        this.enrolledCount = enrolledCount;
                        this.date = date;
                        this.time = time;
                        this.classroom = classroom;
                }

                public String getCourseCode() {
                        return courseCode;
                }

                public int getEnrolledCount() {
                        return enrolledCount;
                }

                public String getDate() {
                        return date;
                }

                public String getTime() {
                        return time;
                }

                public String getClassroom() {
                        return classroom;
                }
        }
}
