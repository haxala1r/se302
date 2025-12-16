package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.se302.model.Classroom;
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.service.DataManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for the Classroom Schedule view.
 * Shows exam assignments for each classroom.
 */
public class ScheduleClassroomController {

        @FXML
        private ComboBox<Classroom> classroomComboBox;
        @FXML
        private Label selectedClassroomLabel;
        @FXML
        private TableView<ClassroomSlotEntry> scheduleTable;
        @FXML
        private TableColumn<ClassroomSlotEntry, String> dateColumn;
        @FXML
        private TableColumn<ClassroomSlotEntry, String> timeColumn;
        @FXML
        private TableColumn<ClassroomSlotEntry, String> courseColumn;
        @FXML
        private TableColumn<ClassroomSlotEntry, Number> studentsColumn;
        @FXML
        private TableColumn<ClassroomSlotEntry, String> utilizationColumn;
        @FXML
        private Label utilizationLabel;

        private DataManager dataManager;

        // Reference to the current schedule state (set by parent controller)
        private Map<String, ExamAssignment> currentAssignments;
        private ScheduleConfiguration configuration;

        @FXML
        public void initialize() {
                dataManager = DataManager.getInstance();

                // Populate classroom combo box
                classroomComboBox.setItems(dataManager.getClassrooms());

                // Set up table columns
                dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
                timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
                courseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCourse()));
                studentsColumn.setCellValueFactory(
                                cellData -> new SimpleIntegerProperty(cellData.getValue().getStudentCount()));
                utilizationColumn.setCellValueFactory(
                                cellData -> new SimpleStringProperty(cellData.getValue().getUtilization()));
        }

        /**
         * Sets the current schedule assignments. Called by parent controller after
         * schedule generation.
         */
        public void setScheduleData(Map<String, ExamAssignment> assignments, ScheduleConfiguration config) {
                this.currentAssignments = assignments;
                this.configuration = config;
        }

        @FXML
        private void onShowSchedule() {
                Classroom selected = classroomComboBox.getValue();
                if (selected == null)
                        return;

                selectedClassroomLabel.setText("Schedule for: " + selected.getClassroomId() +
                                " (Capacity: " + selected.getCapacity() + ")");

                ObservableList<ClassroomSlotEntry> entries = FXCollections.observableArrayList();

                // If we have assignments, filter by this classroom
                if (currentAssignments != null && !currentAssignments.isEmpty()) {
                        for (ExamAssignment assignment : currentAssignments.values()) {
                                if (assignment.isAssigned() &&
                                                selected.getClassroomId().equals(assignment.getClassroomId())) {

                                        // Format date
                                        String dateStr;
                                        if (configuration != null && configuration.getStartDate() != null) {
                                                LocalDate examDate = configuration.getStartDate()
                                                                .plusDays(assignment.getDay());
                                                dateStr = examDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                        } else {
                                                dateStr = "Day " + (assignment.getDay() + 1);
                                        }

                                        // Format time
                                        String timeStr = "Slot " + (assignment.getTimeSlotIndex() + 1);

                                        entries.add(new ClassroomSlotEntry(
                                                        dateStr, timeStr, assignment.getCourseCode(),
                                                        assignment.getStudentCount(), "-"));
                                }
                        }
                }

                scheduleTable.setItems(entries);
                utilizationLabel.setText("Exams shown: " + entries.size());
        }

        // Helper class for table entries
        public static class ClassroomSlotEntry {
                private final String date;
                private final String time;
                private final String course;
                private final int studentCount;
                private final String utilization;

                public ClassroomSlotEntry(String date, String time, String course, int studentCount,
                                String utilization) {
                        this.date = date;
                        this.time = time;
                        this.course = course;
                        this.studentCount = studentCount;
                        this.utilization = utilization;
                }

                public String getDate() {
                        return date;
                }

                public String getTime() {
                        return time;
                }

                public String getCourse() {
                        return course;
                }

                public int getStudentCount() {
                        return studentCount;
                }

                public String getUtilization() {
                        return utilization;
                }
        }
}
