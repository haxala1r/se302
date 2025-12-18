package org.example.se302.controller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import org.example.se302.model.*;
import org.example.se302.service.ConstraintValidationService;
import org.example.se302.service.ConstraintValidationService.ValidationResult;
import org.example.se302.service.DataManager;

import java.util.Optional;

/**
 * Dialog for editing an exam assignment.
 * Provides real-time constraint validation with detailed violation info.
 */
public class ExamEditDialog extends Dialog<ExamEditDialog.EditResult> {

    private final String courseCode;
    private final ScheduleState scheduleState;
    private final ScheduleConfiguration config;
    private final DataManager dataManager;
    private final ConstraintValidationService validationService;

    // UI Components
    private ComboBox<Integer> dayComboBox;
    private ComboBox<Integer> slotComboBox;
    private ComboBox<Classroom> classroomComboBox;
    private VBox validationPanel;
    private Label validationStatusLabel;
    private TextArea violationDetails;
    private Button applyButton;
    private Button applyAnywayButton;

    // Current state
    private ValidationResult currentValidation;

    public ExamEditDialog(ExamAssignment assignment, ScheduleState scheduleState, ScheduleConfiguration config) {
        this.courseCode = assignment.getCourseCode();
        this.scheduleState = scheduleState;
        this.config = config;
        this.dataManager = DataManager.getInstance();
        this.validationService = new ConstraintValidationService();

        setTitle("Exam Details");
        setHeaderText(courseCode);

        buildUI(assignment);
        setupValidation();
        setupButtons();

        // Initial validation
        validateCurrentSelection();
    }

    private void buildUI(ExamAssignment assignment) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // Course info header
        Course course = dataManager.getCourse(courseCode);
        Classroom currentRoom = dataManager.getClassroom(assignment.getClassroomId());
        int studentCount = course != null ? course.getEnrolledStudentsCount() : 0;

        Label courseLabel = new Label("Course: " + courseCode);
        courseLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label studentLabel = new Label("Students enrolled: " + studentCount);
        studentLabel.setStyle("-fx-text-fill: #666;");

        // Current assignment info
        String currentInfo = String.format("Current: Day %d, Slot %d, Room %s",
                assignment.getDay() + 1, assignment.getTimeSlotIndex() + 1, assignment.getClassroomId());
        Label currentLabel = new Label(currentInfo);
        currentLabel.setStyle("-fx-text-fill: #666;");

        // Capacity info
        String capacityInfo = "";
        if (currentRoom != null) {
            double utilization = (double) studentCount / currentRoom.getCapacity() * 100;
            capacityInfo = String.format("Capacity: %d/%d (%.0f%% utilization)",
                    studentCount, currentRoom.getCapacity(), utilization);
        }
        Label capacityLabel = new Label(capacityInfo);
        capacityLabel.setStyle("-fx-text-fill: #666;");

        VBox headerBox = new VBox(5, courseLabel, studentLabel, currentLabel, capacityLabel);
        headerBox.setStyle("-fx-padding: 5; -fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        grid.add(headerBox, 0, 0, 2, 1);

        // Separator
        Separator sep = new Separator();
        grid.add(sep, 0, 1, 2, 1);

        // Day selector
        grid.add(new Label("Day:"), 0, 2);
        dayComboBox = new ComboBox<>();
        for (int i = 0; i < config.getNumDays(); i++) {
            dayComboBox.getItems().add(i);
        }
        dayComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer day) {
                if (day == null)
                    return "";
                TimeSlot slot = config.getTimeSlot(day, 0);
                String dateStr = slot != null ? " (" + slot.getDate().toString() + ")" : "";
                return "Day " + (day + 1) + dateStr;
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        dayComboBox.setValue(assignment.getDay());
        dayComboBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(dayComboBox, 1, 2);

        // Time slot selector
        grid.add(new Label("Time Slot:"), 0, 3);
        slotComboBox = new ComboBox<>();
        for (int i = 0; i < config.getSlotsPerDay(); i++) {
            slotComboBox.getItems().add(i);
        }
        slotComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer slot) {
                if (slot == null)
                    return "";
                TimeSlot ts = config.getTimeSlot(0, slot);
                String timeStr = ts != null ? " (" + ts.getStartTime() + " - " + ts.getEndTime() + ")" : "";
                return "Slot " + (slot + 1) + timeStr;
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        slotComboBox.setValue(assignment.getTimeSlotIndex());
        slotComboBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(slotComboBox, 1, 3);

        // Classroom selector
        grid.add(new Label("Classroom:"), 0, 4);
        classroomComboBox = new ComboBox<>();
        classroomComboBox.getItems().addAll(dataManager.getClassrooms());
        classroomComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Classroom classroom) {
                if (classroom == null)
                    return "";
                return classroom.getClassroomId() + " (Capacity: " + classroom.getCapacity() + ")";
            }

            @Override
            public Classroom fromString(String string) {
                return null;
            }
        });
        // Set current classroom
        Classroom currentClassroom = dataManager.getClassroom(assignment.getClassroomId());
        classroomComboBox.setValue(currentClassroom);
        classroomComboBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(classroomComboBox, 1, 4);

        // Column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(250);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Separator before validation
        Separator sep2 = new Separator();
        grid.add(sep2, 0, 5, 2, 1);

        // Validation panel
        validationPanel = new VBox(10);
        validationPanel.setPadding(new Insets(10));
        validationPanel.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        validationStatusLabel = new Label("Checking constraints...");
        validationStatusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        violationDetails = new TextArea();
        violationDetails.setEditable(false);
        violationDetails.setWrapText(true);
        violationDetails.setPrefRowCount(4);
        violationDetails.setMaxHeight(120);
        violationDetails.setStyle("-fx-control-inner-background: #f5f5f5;");

        validationPanel.getChildren().addAll(validationStatusLabel, violationDetails);
        grid.add(validationPanel, 0, 6, 2, 1);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(450);
    }

    private void setupValidation() {
        // Add listeners to all inputs
        dayComboBox.valueProperty().addListener((obs, old, newVal) -> validateCurrentSelection());
        slotComboBox.valueProperty().addListener((obs, old, newVal) -> validateCurrentSelection());
        classroomComboBox.valueProperty().addListener((obs, old, newVal) -> validateCurrentSelection());
    }

    private void validateCurrentSelection() {
        Integer day = dayComboBox.getValue();
        Integer slot = slotComboBox.getValue();
        Classroom classroom = classroomComboBox.getValue();

        if (day == null || slot == null || classroom == null) {
            validationStatusLabel.setText("⚠️ Please select all fields");
            validationStatusLabel.setTextFill(Color.ORANGE);
            violationDetails.setText("");
            updateButtonStates(false, false);
            return;
        }

        // Perform validation
        currentValidation = validationService.validateAssignment(
                courseCode, day, slot, classroom.getClassroomId(), scheduleState);

        if (currentValidation.isValid()) {
            validationStatusLabel.setText("✓ No constraint violations");
            validationStatusLabel.setTextFill(Color.web("#27ae60"));
            violationDetails.setText("");
            validationPanel.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 5;");
            updateButtonStates(true, false);
        } else if (currentValidation.hasHardViolations()) {
            validationStatusLabel.setText("Constraint violations found!");
            validationStatusLabel.setTextFill(Color.web("#e74c3c"));
            violationDetails.setText(currentValidation.getFormattedMessage());
            validationPanel.setStyle("-fx-background-color: #ffebee; -fx-background-radius: 5;");
            updateButtonStates(false, true);
        } else {
            validationStatusLabel.setText("Warnings found!");
            validationStatusLabel.setTextFill(Color.web("#f39c12"));
            violationDetails.setText(currentValidation.getFormattedMessage());
            validationPanel.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 5;");
            updateButtonStates(true, true);
        }
    }

    private void updateButtonStates(boolean applyEnabled, boolean showApplyAnyway) {
        if (applyButton != null) {
            applyButton.setDisable(!applyEnabled);
        }
        if (applyAnywayButton != null) {
            applyAnywayButton.setVisible(showApplyAnyway);
            applyAnywayButton.setManaged(showApplyAnyway);
        }
    }

    private void setupButtons() {
        // Create custom button types
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, applyButtonType);

        applyButton = (Button) getDialogPane().lookupButton(applyButtonType);
        applyButton.setDefaultButton(true);

        // Add "Apply Anyway" button for when there are violations
        applyAnywayButton = new Button("Apply Anyway");
        applyAnywayButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        applyAnywayButton.setVisible(false);
        applyAnywayButton.setManaged(false);

        // Find the button bar and add our custom button
        ButtonBar buttonBar = (ButtonBar) getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            ButtonBar.setButtonData(applyAnywayButton, ButtonBar.ButtonData.LEFT);
            buttonBar.getButtons().add(0, applyAnywayButton);
        }

        // Handle apply anyway
        applyAnywayButton.setOnAction(e -> {
            if (currentValidation != null && currentValidation.hasHardViolations()) {
                // Show confirmation for hard violations
                Alert confirm = new Alert(Alert.AlertType.WARNING);
                confirm.setTitle("Confirm Override");
                confirm.setHeaderText("Override Constraint Violations?");
                confirm.setContentText(
                        "You are about to apply changes that violate hard constraints:\n\n" +
                                currentValidation.getFormattedMessage() + "\n\n" +
                                "This may cause scheduling conflicts for students. Are you sure?");
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    setResultAndClose(true);
                }
            } else {
                setResultAndClose(true);
            }
        });

        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return createResult(false);
            }
            return null;
        });
    }

    private void setResultAndClose(boolean forced) {
        EditResult result = createResult(forced);
        setResult(result);
        close();
    }

    private EditResult createResult(boolean forced) {
        Integer day = dayComboBox.getValue();
        Integer slot = slotComboBox.getValue();
        Classroom classroom = classroomComboBox.getValue();

        if (day == null || slot == null || classroom == null) {
            return null;
        }

        return new EditResult(courseCode, day, slot, classroom.getClassroomId(), forced);
    }

    /**
     * Result of the edit dialog.
     */
    public static class EditResult {
        private final String courseCode;
        private final int day;
        private final int timeSlot;
        private final String classroomId;
        private final boolean forcedOverride;

        public EditResult(String courseCode, int day, int timeSlot, String classroomId, boolean forcedOverride) {
            this.courseCode = courseCode;
            this.day = day;
            this.timeSlot = timeSlot;
            this.classroomId = classroomId;
            this.forcedOverride = forcedOverride;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public int getDay() {
            return day;
        }

        public int getTimeSlot() {
            return timeSlot;
        }

        public String getClassroomId() {
            return classroomId;
        }

        public boolean isForcedOverride() {
            return forcedOverride;
        }
    }
}
