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
import org.example.se302.service.DataManager;

/**
 * Controller for the Classroom Schedule view.
 */
public class ScheduleClassroomController {

    @FXML private ComboBox<Classroom> classroomComboBox;
    @FXML private Label selectedClassroomLabel;
    @FXML private TableView<ClassroomSlotEntry> scheduleTable;
    @FXML private TableColumn<ClassroomSlotEntry, String> dateColumn;
    @FXML private TableColumn<ClassroomSlotEntry, String> timeColumn;
    @FXML private TableColumn<ClassroomSlotEntry, String> courseColumn;
    @FXML private TableColumn<ClassroomSlotEntry, Number> studentsColumn;
    @FXML private TableColumn<ClassroomSlotEntry, String> utilizationColumn;
    @FXML private Label utilizationLabel;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Populate classroom combo box
        classroomComboBox.setItems(dataManager.getClassrooms());

        // Set up table columns
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate()));
        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTime()));
        courseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCourse()));
        studentsColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getStudentCount()));
        utilizationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUtilization()));
    }

    @FXML
    private void onShowSchedule() {
        Classroom selected = classroomComboBox.getValue();
        if (selected == null) return;

        selectedClassroomLabel.setText("Schedule for: " + selected.getClassroomId() +
                " (Capacity: " + selected.getCapacity() + ")");

        // For demo: show empty schedule
        ObservableList<ClassroomSlotEntry> entries = FXCollections.observableArrayList();
        scheduleTable.setItems(entries);

        utilizationLabel.setText("Overall Utilization: 0% (No exams scheduled)");
    }

    // Helper class for table entries
    public static class ClassroomSlotEntry {
        private final String date;
        private final String time;
        private final String course;
        private final int studentCount;
        private final String utilization;

        public ClassroomSlotEntry(String date, String time, String course, int studentCount, String utilization) {
            this.date = date;
            this.time = time;
            this.course = course;
            this.studentCount = studentCount;
            this.utilization = utilization;
        }

        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getCourse() { return course; }
        public int getStudentCount() { return studentCount; }
        public String getUtilization() { return utilization; }
    }
}
