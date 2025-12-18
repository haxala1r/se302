package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.example.se302.model.Course;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.model.TimeSlot;
import org.example.se302.service.DataManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

// Color palette for days (pastel colors for readability)
// Day 0 = Light Blue, Day 1 = Light Green, Day 2 = Light Yellow, etc.

/**
 * Controller for the Course Schedule view.
 * Displays courses with their exam date, time, and assigned classroom.
 * Supports sorting by date/time.
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

                // Enable custom sorting by day and slot (not alphabetical by string)
                dateColumn.setComparator((date1, date2) -> {
                        // "Not Scheduled" should always be last
                        if (date1.equals("Not Scheduled"))
                                return 1;
                        if (date2.equals("Not Scheduled"))
                                return -1;
                        return date1.compareTo(date2);
                });

                timeColumn.setComparator((time1, time2) -> {
                        // Extract slot number for proper numeric sorting
                        if (time1.equals("-"))
                                return 1;
                        if (time2.equals("-"))
                                return -1;
                        try {
                                int slot1 = Integer.parseInt(time1.replace("Slot ", ""));
                                int slot2 = Integer.parseInt(time2.replace("Slot ", ""));
                                return Integer.compare(slot1, slot2);
                        } catch (NumberFormatException e) {
                                return time1.compareTo(time2);
                        }
                });

                // Color-code rows by day
                courseScheduleTable.setRowFactory(tv -> new TableRow<CourseScheduleEntry>() {
                        // Pastel color palette for different days
                        private final String[] dayColors = {
                                        "#E3F2FD", // Day 0 - Light Blue
                                        "#E8F5E9", // Day 1 - Light Green
                                        "#FFF8E1", // Day 2 - Light Yellow
                                        "#FCE4EC", // Day 3 - Light Pink
                                        "#F3E5F5", // Day 4 - Light Purple
                                        "#E0F7FA", // Day 5 - Light Cyan
                                        "#FBE9E7" // Day 6 - Light Orange
                        };

                        @Override
                        protected void updateItem(CourseScheduleEntry item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                        setStyle("");
                                } else if (item.getDayIndex() == Integer.MAX_VALUE) {
                                        // Not scheduled - gray background
                                        setStyle("-fx-background-color: #F5F5F5;");
                                } else {
                                        // Color by day (cycle through palette)
                                        int colorIndex = item.getDayIndex() % dayColors.length;
                                        setStyle("-fx-background-color: " + dayColors[colorIndex] + ";");
                                }
                        }
                });

                // Load data
                loadScheduleData();

                // Listen for data changes
                dataManager.getCourses().addListener(
                                (javafx.collections.ListChangeListener<Course>) c -> loadScheduleData());

                // Refresh when tab is selected - find TabPane once scene is available
                courseScheduleTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                                loadScheduleData();
                                // Find parent TabPane and listen for selection changes
                                javafx.scene.Parent parent = courseScheduleTable.getParent();
                                while (parent != null) {
                                        if (parent.getParent() instanceof javafx.scene.control.TabPane) {
                                                javafx.scene.control.TabPane tabPane = (javafx.scene.control.TabPane) parent
                                                                .getParent();
                                                // Find which tab contains our content
                                                for (javafx.scene.control.Tab tab : tabPane.getTabs()) {
                                                        if (tab.getContent() == parent) {
                                                                tab.selectedProperty().addListener(
                                                                                (o, wasSelected, isSelected) -> {
                                                                                        if (isSelected)
                                                                                                loadScheduleData();
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

        private void loadScheduleData() {
                ScheduleConfiguration config = dataManager.getActiveConfiguration();
                ObservableList<CourseScheduleEntry> entries = FXCollections.observableArrayList();

                for (Course course : dataManager.getCourses()) {
                        String courseCode = course.getCourseCode();
                        int enrolled = course.getEnrolledStudentsCount();

                        String dateStr = "Not Scheduled";
                        String timeStr = "-";
                        String classroomStr = "-";
                        int dayIndex = Integer.MAX_VALUE;
                        int slotIndex = Integer.MAX_VALUE;

                        // Check if this course has been scheduled
                        if (course.isScheduled()) {
                                dayIndex = course.getExamDay();
                                slotIndex = course.getExamTimeSlot();
                                classroomStr = course.getAssignedClassroom();

                                // Format date using configuration's start date
                                if (config != null && config.getStartDate() != null) {
                                        LocalDate examDate = config.getStartDate().plusDays(dayIndex);
                                        dateStr = examDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)"));
                                } else {
                                        dateStr = "Day " + (dayIndex + 1);
                                }

                                // Format time using configuration's time slots
                                if (config != null) {
                                        TimeSlot timeSlot = config.getTimeSlot(dayIndex, slotIndex);
                                        if (timeSlot != null) {
                                                timeStr = timeSlot.getStartTime() + " - " + timeSlot.getEndTime();
                                        } else {
                                                timeStr = "Slot " + (slotIndex + 1);
                                        }
                                } else {
                                        timeStr = "Slot " + (slotIndex + 1);
                                }
                        }

                        entries.add(new CourseScheduleEntry(courseCode, enrolled, dateStr, timeStr, classroomStr,
                                        dayIndex, slotIndex));
                }

                // Sort by day first, then by slot
                entries.sort(Comparator.comparingInt(CourseScheduleEntry::getDayIndex)
                                .thenComparingInt(CourseScheduleEntry::getSlotIndex));

                courseScheduleTable.setItems(entries);
        }

        // Helper class for table entries
        public static class CourseScheduleEntry {
                private final String courseCode;
                private final int enrolledCount;
                private final String date;
                private final String time;
                private final String classroom;
                private final int dayIndex;
                private final int slotIndex;

                public CourseScheduleEntry(String courseCode, int enrolledCount, String date, String time,
                                String classroom, int dayIndex, int slotIndex) {
                        this.courseCode = courseCode;
                        this.enrolledCount = enrolledCount;
                        this.date = date;
                        this.time = time;
                        this.classroom = classroom;
                        this.dayIndex = dayIndex;
                        this.slotIndex = slotIndex;
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

                public int getDayIndex() {
                        return dayIndex;
                }

                public int getSlotIndex() {
                        return slotIndex;
                }
        }

        /**
         * Exports the course schedule as a CSV file.
         */
        @FXML
        private void onExportCSV() {
                if (courseScheduleTable.getItems().isEmpty()) {
                        showAlert(javafx.scene.control.Alert.AlertType.WARNING, "No Data",
                                        "No schedule data to export.");
                        return;
                }

                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Export Course Schedule as CSV");
                fileChooser.getExtensionFilters().add(
                                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                fileChooser.setInitialFileName("schedule_courses.csv");

                java.io.File file = fileChooser.showSaveDialog(courseScheduleTable.getScene().getWindow());
                if (file == null)
                        return;

                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                        // Header
                        writer.println("Course Code,Date,Time,Classroom,Enrolled Students");

                        // Data rows
                        for (CourseScheduleEntry entry : courseScheduleTable.getItems()) {
                                writer.println(String.format("%s,\"%s\",%s,%s,%d",
                                                entry.getCourseCode(),
                                                entry.getDate(),
                                                entry.getTime(),
                                                entry.getClassroom(),
                                                entry.getEnrolledCount()));
                        }

                        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Export Complete",
                                        "Course schedule exported to:\n" + file.getAbsolutePath());
                } catch (java.io.IOException e) {
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Export Failed",
                                        "Could not write file: " + e.getMessage());
                }
        }

        private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String message) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
        }
}
