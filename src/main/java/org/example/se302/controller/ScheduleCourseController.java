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
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.service.DataManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;

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
                        int dayIndex = Integer.MAX_VALUE; // For sorting unscheduled items last
                        int slotIndex = Integer.MAX_VALUE;

                        // Check if we have an assignment for this course
                        if (currentAssignments != null && currentAssignments.containsKey(courseCode)) {
                                ExamAssignment assignment = currentAssignments.get(courseCode);

                                if (assignment.isAssigned()) {
                                        dayIndex = assignment.getDay();
                                        slotIndex = assignment.getTimeSlotIndex();

                                        // Format date based on configuration start date + day offset
                                        if (configuration != null && configuration.getStartDate() != null) {
                                                LocalDate examDate = configuration.getStartDate().plusDays(dayIndex);
                                                dateStr = examDate.format(
                                                                DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)"));
                                        } else {
                                                dateStr = "Day " + (dayIndex + 1);
                                        }

                                        // Format time slot
                                        timeStr = "Slot " + (slotIndex + 1);

                                        // Classroom
                                        classroomStr = assignment.getClassroomId() != null ? assignment.getClassroomId()
                                                        : "-";
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
}
