package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.example.se302.model.Classroom;
import org.example.se302.model.Course;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.service.DataManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

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

                // Custom comparators for proper sorting
                dateColumn.setComparator((d1, d2) -> {
                        if (d1.startsWith("Day") && d2.startsWith("Day")) {
                                try {
                                        int day1 = Integer.parseInt(d1.replace("Day ", ""));
                                        int day2 = Integer.parseInt(d2.replace("Day ", ""));
                                        return Integer.compare(day1, day2);
                                } catch (NumberFormatException e) {
                                        return d1.compareTo(d2);
                                }
                        }
                        return d1.compareTo(d2);
                });

                timeColumn.setComparator((t1, t2) -> {
                        try {
                                int slot1 = Integer.parseInt(t1.replace("Slot ", ""));
                                int slot2 = Integer.parseInt(t2.replace("Slot ", ""));
                                return Integer.compare(slot1, slot2);
                        } catch (NumberFormatException e) {
                                return t1.compareTo(t2);
                        }
                });

                // Color-code rows by utilization percentage
                scheduleTable.setRowFactory(tv -> new TableRow<ClassroomSlotEntry>() {
                        @Override
                        protected void updateItem(ClassroomSlotEntry item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                        setStyle("");
                                } else {
                                        int utilPercent = item.getUtilizationPercent();
                                        if (utilPercent >= 90) {
                                                setStyle("-fx-background-color: #FFCDD2;"); // Red - high
                                        } else if (utilPercent >= 70) {
                                                setStyle("-fx-background-color: #FFF9C4;"); // Yellow - medium
                                        } else if (utilPercent > 0) {
                                                setStyle("-fx-background-color: #C8E6C9;"); // Green - low
                                        } else {
                                                setStyle("-fx-background-color: #F5F5F5;"); // Gray - empty
                                        }
                                }
                        }
                });

                // Refresh when tab is selected - find TabPane once scene is available
                scheduleTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                                // Find parent TabPane and listen for selection changes
                                javafx.scene.Parent parent = scheduleTable.getParent();
                                while (parent != null) {
                                        if (parent.getParent() instanceof javafx.scene.control.TabPane) {
                                                javafx.scene.control.TabPane tabPane = (javafx.scene.control.TabPane) parent
                                                                .getParent();
                                                // Find which tab contains our content
                                                for (javafx.scene.control.Tab tab : tabPane.getTabs()) {
                                                        if (tab.getContent() == parent) {
                                                                tab.selectedProperty().addListener(
                                                                                (o, wasSelected, isSelected) -> {
                                                                                        if (isSelected && classroomComboBox
                                                                                                        .getValue() != null) {
                                                                                                onShowSchedule();
                                                                                        }
                                                                                });
                                                                break;
                                                        }
                                                }
                                                break;
                                        }
                                        parent = parent.getParent();
                                }
                        }
                });
        }

        @FXML
        private void onShowSchedule() {
                Classroom selected = classroomComboBox.getValue();
                if (selected == null)
                        return;

                selectedClassroomLabel.setText("Schedule for: " + selected.getClassroomId() +
                                " (Capacity: " + selected.getCapacity() + ")");

                ScheduleConfiguration config = dataManager.getActiveConfiguration();
                ObservableList<ClassroomSlotEntry> entries = FXCollections.observableArrayList();
                int totalSlots = 0;
                int usedSlots = 0;
                int totalStudents = 0;

                // Find all courses scheduled in this classroom
                for (Course course : dataManager.getCourses()) {
                        if (course.isScheduled() &&
                                        selected.getClassroomId().equals(course.getAssignedClassroom())) {

                                int dayIndex = course.getExamDay();
                                int slotIndex = course.getExamTimeSlot();

                                // Format date
                                String dateStr;
                                if (config != null && config.getStartDate() != null) {
                                        LocalDate examDate = config.getStartDate().plusDays(dayIndex);
                                        dateStr = examDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                } else {
                                        dateStr = "Day " + (dayIndex + 1);
                                }

                                String timeStr = "Slot " + (slotIndex + 1);

                                // Calculate utilization
                                int studentCount = course.getEnrolledStudentsCount();
                                int capacity = selected.getCapacity();
                                int utilizationPercent = capacity > 0 ? (studentCount * 100) / capacity : 0;
                                String utilizationStr = utilizationPercent + "%";

                                entries.add(new ClassroomSlotEntry(
                                                dateStr, timeStr, course.getCourseCode(),
                                                studentCount, utilizationStr, utilizationPercent,
                                                dayIndex, slotIndex));

                                usedSlots++;
                                totalStudents += studentCount;
                        }
                }

                // Calculate total possible slots
                if (config != null) {
                        totalSlots = config.getNumDays() * config.getSlotsPerDay();
                }

                // Sort by day then slot
                entries.sort(Comparator.comparingInt(ClassroomSlotEntry::getDayIndex)
                                .thenComparingInt(ClassroomSlotEntry::getSlotIndex));

                scheduleTable.setItems(entries);

                // Update overall utilization label
                if (totalSlots > 0) {
                        int overallUtilization = (usedSlots * 100) / totalSlots;
                        utilizationLabel.setText(String.format(
                                        "Overall Utilization: %d%% (%d/%d slots used, %d total students)",
                                        overallUtilization, usedSlots, totalSlots, totalStudents));
                } else {
                        utilizationLabel.setText("Overall Utilization: 0% (No schedule data available)");
                }
        }

        // Helper class for table entries
        public static class ClassroomSlotEntry {
                private final String date;
                private final String time;
                private final String course;
                private final int studentCount;
                private final String utilization;
                private final int utilizationPercent;
                private final int dayIndex;
                private final int slotIndex;

                public ClassroomSlotEntry(String date, String time, String course, int studentCount,
                                String utilization, int utilizationPercent, int dayIndex, int slotIndex) {
                        this.date = date;
                        this.time = time;
                        this.course = course;
                        this.studentCount = studentCount;
                        this.utilization = utilization;
                        this.utilizationPercent = utilizationPercent;
                        this.dayIndex = dayIndex;
                        this.slotIndex = slotIndex;
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

                public int getUtilizationPercent() {
                        return utilizationPercent;
                }

                public int getDayIndex() {
                        return dayIndex;
                }

                public int getSlotIndex() {
                        return slotIndex;
                }
        }
}
