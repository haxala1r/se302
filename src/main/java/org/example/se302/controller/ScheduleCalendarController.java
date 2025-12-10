package org.example.se302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;

/**
 * Controller for the Calendar/Day schedule view.
 */
public class ScheduleCalendarController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML
    public void initialize() {
        // Initialize with default date range if needed
    }

    @FXML
    private void onGenerateSchedule() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Schedule Generation");
        alert.setContentText("The CSP-based schedule generation algorithm will be implemented in Phase 2.\n\n" +
                "It will automatically create an exam schedule that satisfies all constraints:\n" +
                "- No consecutive exams for students\n" +
                "- Max 2 exams per day per student\n" +
                "- Classroom capacity limits\n" +
                "- No double-booking");
        alert.showAndWait();
    }
}
