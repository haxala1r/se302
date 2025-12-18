package org.example.se302.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import org.example.se302.model.*;
import org.example.se302.service.ConstraintValidationService;
import org.example.se302.service.DataManager;
import org.example.se302.service.ScheduleGeneratorService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Controller for the Calendar/Day schedule view.
 * Handles schedule configuration and generation using CSP algorithm.
 */
public class ScheduleCalendarController {

    // Configuration controls
    @FXML
    private Spinner<Integer> numDaysSpinner;
    @FXML
    private Spinner<Integer> slotsPerDaySpinner;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private Spinner<Integer> slotDurationSpinner;
    @FXML
    private ComboBox<String> strategyComboBox;
    @FXML
    private ComboBox<String> startTimeComboBox;
    @FXML
    private CheckBox allowBackToBackCheckBox;
    @FXML
    private Label summaryLabel;

    // Action controls
    @FXML
    private Button generateButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label statusLabel;

    // Progress controls
    @FXML
    private VBox progressContainer;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private Label progressDetailLabel;

    // Schedule display
    @FXML
    private ScrollPane scheduleScrollPane;
    @FXML
    private GridPane scheduleGrid;

    // Statistics labels
    @FXML
    private Label totalCoursesLabel;
    @FXML
    private Label scheduledCoursesLabel;
    @FXML
    private Label classroomsUsedLabel;
    @FXML
    private Label generationTimeLabel;

    // Services
    private final DataManager dataManager = DataManager.getInstance();
    private final ConstraintValidationService validationService = new ConstraintValidationService();
    private ScheduleGeneratorService generatorService;
    private ScheduleState currentSchedule;
    private ScheduleConfiguration currentConfig;
    private Thread generationThread;

    // Drag-and-drop state
    private static final DataFormat EXAM_DATA_FORMAT = new DataFormat("application/x-exam-assignment");
    private ExamAssignment draggedExam = null;
    private Map<String, VBox> cellMap = new HashMap<>(); // "day_slot" -> cell VBox

    @FXML
    public void initialize() {
        // Initialize spinners
        initializeSpinners();

        // Initialize combo boxes
        initializeComboBoxes();

        // Set default date to next week
        startDatePicker.setValue(LocalDate.now().plusDays(7));

        // Set default checkbox - student-friendly (no back-to-back exams)
        allowBackToBackCheckBox.setSelected(false);

        // Add listeners to update summary
        numDaysSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateSummary());
        slotsPerDaySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateSummary());

        // Initial summary update
        updateSummary();

        // Update status with data info
        updateDataStatus();
    }

    private void initializeSpinners() {
        // Number of days spinner (1-30)
        SpinnerValueFactory<Integer> daysFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5);
        numDaysSpinner.setValueFactory(daysFactory);

        // Slots per day spinner (1-10)
        SpinnerValueFactory<Integer> slotsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4);
        slotsPerDaySpinner.setValueFactory(slotsFactory);

        // Slot duration spinner (30-240 minutes)
        SpinnerValueFactory<Integer> durationFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 240, 120,
                30);
        slotDurationSpinner.setValueFactory(durationFactory);
    }

    private void initializeComboBoxes() {
        // Optimization strategies - consolidated to 3 meaningful options
        List<String> strategies = Arrays.asList(
                "Student Friendly (Default)",
                "Minimize Days",
                "Minimize Classrooms");
        strategyComboBox.setItems(FXCollections.observableArrayList(strategies));
        strategyComboBox.getSelectionModel().selectFirst(); // Default: Student Friendly

        // Start times (8:00 - 14:00)
        List<String> times = Arrays.asList(
                "08:00", "08:30", "09:00", "09:30", "10:00");
        startTimeComboBox.setItems(FXCollections.observableArrayList(times));
        startTimeComboBox.getSelectionModel().select("09:00");
    }

    private void updateSummary() {
        int days = numDaysSpinner.getValue();
        int slots = slotsPerDaySpinner.getValue();
        int total = days * slots;
        summaryLabel.setText(String.format("%d days × %d slots = %d total time slots", days, slots, total));
    }

    private void updateDataStatus() {
        int courses = dataManager.getTotalCourses();
        int classrooms = dataManager.getTotalClassrooms();
        int students = dataManager.getTotalStudents();

        if (courses == 0 || classrooms == 0) {
            statusLabel
                    .setText("⚠️ Please import data first (Courses: " + courses + ", Classrooms: " + classrooms + ")");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else {
            statusLabel.setText(
                    "✓ Ready - " + courses + " courses, " + classrooms + " classrooms, " + students + " students");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        }

        totalCoursesLabel.setText(String.valueOf(courses));
    }

    @FXML
    private void onGenerateSchedule() {
        // Validate data
        if (dataManager.getTotalCourses() == 0) {
            showAlert(Alert.AlertType.WARNING, "No Data",
                    "Please import course data before generating a schedule.");
            return;
        }

        if (dataManager.getTotalClassrooms() == 0) {
            showAlert(Alert.AlertType.WARNING, "No Classrooms",
                    "Please import classroom data before generating a schedule.");
            return;
        }

        // Validate configuration
        if (startDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Date",
                    "Please select a start date for the exam period.");
            return;
        }

        // Build configuration
        ScheduleConfiguration config = buildConfiguration();

        // Validate total slots
        int totalSlots = config.getTotalSlots();
        int totalCourses = dataManager.getTotalCourses();
        int totalClassrooms = dataManager.getTotalClassrooms();

        if (totalSlots * totalClassrooms < totalCourses) {
            showAlert(Alert.AlertType.WARNING, "Insufficient Capacity",
                    String.format(
                            "Not enough time slots! You have %d courses but only %d total capacity (%d slots × %d classrooms).\n\nPlease increase days or slots per day.",
                            totalCourses, totalSlots * totalClassrooms, totalSlots, totalClassrooms));
            return;
        }

        // Start generation
        startGeneration(config);
    }

    private ScheduleConfiguration buildConfiguration() {
        ScheduleConfiguration config = new ScheduleConfiguration();

        config.setNumDays(numDaysSpinner.getValue());
        config.setSlotsPerDay(slotsPerDaySpinner.getValue());
        config.setStartDate(startDatePicker.getValue());
        config.setSlotDurationMinutes(slotDurationSpinner.getValue());
        config.setAllowBackToBackExams(allowBackToBackCheckBox.isSelected());

        // Parse start time
        String timeStr = startTimeComboBox.getValue();
        if (timeStr != null) {
            String[] parts = timeStr.split(":");
            config.setDayStartTime(LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
        }

        // Set optimization strategy (consolidated to 3 options)
        String strategyStr = strategyComboBox.getValue();
        ScheduleConfiguration.OptimizationStrategy strategy = ScheduleConfiguration.OptimizationStrategy.STUDENT_FRIENDLY;

        if (strategyStr != null) {
            if (strategyStr.contains("Minimize Days")) {
                strategy = ScheduleConfiguration.OptimizationStrategy.MINIMIZE_DAYS;
            } else if (strategyStr.contains("Minimize Classrooms")) {
                strategy = ScheduleConfiguration.OptimizationStrategy.MINIMIZE_CLASSROOMS;
            }
            // STUDENT_FRIENDLY is default, no explicit check needed
        }
        config.setOptimizationStrategy(strategy);

        return config;
    }

    private void startGeneration(ScheduleConfiguration config) {
        // Update UI for generation
        generateButton.setDisable(true);
        cancelButton.setDisable(false);
        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        progressBar.setProgress(0);
        if (progressIndicator != null)
            progressIndicator.setProgress(-1.0); // Indeterminate
        progressLabel.setText("Generating Schedule...");
        if (progressDetailLabel != null)
            progressDetailLabel.setText("Initializing CSP solver...");
        statusLabel.setText("⏳ Generating schedule...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        long startTime = System.currentTimeMillis();

        // Create generator service
        generatorService = new ScheduleGeneratorService();
        generatorService.setProgressListener((progress, message) -> {
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                if (progressIndicator != null)
                    progressIndicator.setProgress(progress);
                if (progressDetailLabel != null)
                    progressDetailLabel.setText(message);
            });
        });

        // Run generation in background thread
        generationThread = new Thread(() -> {
            ScheduleGeneratorService.ScheduleResult result = generatorService.generateSchedule(config);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Platform.runLater(() -> {
                onGenerationComplete(result, config, duration);
            });
        });

        generationThread.setDaemon(true);
        generationThread.start();
    }

    private void onGenerationComplete(ScheduleGeneratorService.ScheduleResult result,
            ScheduleConfiguration config, long durationMs) {
        // Reset UI
        generateButton.setDisable(false);
        cancelButton.setDisable(true);
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);

        if (result.isSuccess()) {
            currentSchedule = result.getScheduleState();
            currentConfig = config;

            // Save schedule to DataManager (so other views can see it)
            saveScheduleToDataManager(currentSchedule, config);

            // Update status
            statusLabel.setText("✓ Schedule generated successfully!");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");

            // Update statistics
            updateStatistics(currentSchedule, durationMs);

            // Display schedule in grid
            displayScheduleGrid(currentSchedule, config);

            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Exam schedule generated successfully!\n\n" +
                            "Scheduled: " + currentSchedule.getAssignedCourses() + "/"
                            + currentSchedule.getTotalCourses() + " courses\n" +
                            "Time: " + durationMs + "ms\n\n" +
                            "The schedule has been saved. You can now view individual schedules in 'Student Schedule' and 'Course Schedule' tabs.");
        } else if (result.wasCancelled()) {
            statusLabel.setText("⚠️ Generation cancelled");
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        } else {
            statusLabel.setText("❌ Generation failed");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");

            showAlert(Alert.AlertType.ERROR, "Generation Failed",
                    result.getMessage()
                            + "\n\nTry:\n• Increasing number of days\n• Increasing slots per day\n• Adding more classrooms\n• Switching to 'Allow back-to-back exams'");
        }
    }

    private void saveScheduleToDataManager(ScheduleState schedule, ScheduleConfiguration config) {
        // 1. Save configuration
        dataManager.setActiveConfiguration(config);

        // 2. Clear old schedule data from courses
        for (Course course : dataManager.getCourses()) {
            course.setExamSchedule(-1, -1, null);
        }

        // 3. Apply new schedule
        for (ExamAssignment assignment : schedule.getAssignments().values()) {
            if (assignment.isAssigned()) {
                Course course = dataManager.getCourse(assignment.getCourseCode());
                if (course != null) {
                    course.setExamSchedule(
                            assignment.getDay(),
                            assignment.getTimeSlotIndex(),
                            assignment.getClassroomId());
                }
            }
        }
    }

    private void updateStatistics(ScheduleState schedule, long durationMs) {
        scheduledCoursesLabel.setText(String.valueOf(schedule.getAssignedCourses()));

        // Count unique classrooms used
        Set<String> usedClassrooms = new HashSet<>();
        for (ExamAssignment assignment : schedule.getAssignments().values()) {
            if (assignment.isAssigned() && assignment.getClassroomId() != null) {
                usedClassrooms.add(assignment.getClassroomId());
            }
        }
        classroomsUsedLabel.setText(String.valueOf(usedClassrooms.size()));

        // Format generation time
        if (durationMs < 1000) {
            generationTimeLabel.setText(durationMs + "ms");
        } else {
            generationTimeLabel.setText(String.format("%.2fs", durationMs / 1000.0));
        }
    }

    private void displayScheduleGrid(ScheduleState schedule, ScheduleConfiguration config) {
        scheduleGrid.getChildren().clear();
        scheduleGrid.getColumnConstraints().clear();
        scheduleGrid.getRowConstraints().clear();
        cellMap.clear(); // Clear cell map for drag-and-drop

        int numDays = config.getNumDays();
        int slotsPerDay = config.getSlotsPerDay();

        // Column constraints - make columns wider for better visibility
        ColumnConstraints headerCol = new ColumnConstraints();
        headerCol.setMinWidth(100);
        headerCol.setPrefWidth(120);
        scheduleGrid.getColumnConstraints().add(headerCol);

        for (int day = 0; day < numDays; day++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setMinWidth(180);
            dayCol.setPrefWidth(220);
            dayCol.setHgrow(Priority.ALWAYS);
            scheduleGrid.getColumnConstraints().add(dayCol);
        }

        // Row constraints - make rows taller for better visibility
        for (int slot = 0; slot <= slotsPerDay; slot++) {
            RowConstraints rowConstraint = new RowConstraints();
            rowConstraint.setMinHeight(80);
            rowConstraint.setPrefHeight(100);
            rowConstraint.setVgrow(Priority.SOMETIMES);
            scheduleGrid.getRowConstraints().add(rowConstraint);
        }

        // Header row - Days
        Label cornerLabel = createHeaderLabel("");
        scheduleGrid.add(cornerLabel, 0, 0);

        for (int day = 0; day < numDays; day++) {
            Label dayLabel = createHeaderLabel("Day " + (day + 1) + "\n" +
                    config.getStartDate().plusDays(day).toString());
            scheduleGrid.add(dayLabel, day + 1, 0);
        }

        // Time slot rows
        for (int slot = 0; slot < slotsPerDay; slot++) {
            // Time slot label
            TimeSlot timeSlot = config.getTimeSlot(0, slot);
            String timeText = timeSlot != null ? timeSlot.getStartTime() + "\n-\n" + timeSlot.getEndTime()
                    : "Slot " + (slot + 1);
            Label slotLabel = createHeaderLabel(timeText);
            scheduleGrid.add(slotLabel, 0, slot + 1);

            // Cells for each day
            for (int day = 0; day < numDays; day++) {
                VBox cellContent = createScheduleCell(schedule, day, slot);
                scheduleGrid.add(cellContent, day + 1, slot + 1);

                // Store cell reference for drag-and-drop
                cellMap.put(day + "_" + slot, cellContent);
            }
        }
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; " +
                "-fx-padding: 10; -fx-alignment: center;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private VBox createScheduleCell(ScheduleState schedule, int day, int slot) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(5));
        cell.setAlignment(Pos.TOP_LEFT);
        String defaultCellStyle = "-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;";
        cell.setStyle(defaultCellStyle);

        // Store cell coordinates for drop handling
        cell.setUserData(new int[] { day, slot });

        // Find all exams at this day/slot
        List<ExamAssignment> examsAtSlot = new ArrayList<>();
        for (ExamAssignment assignment : schedule.getAssignments().values()) {
            if (assignment.isAssigned() &&
                    assignment.getDay() == day &&
                    assignment.getTimeSlotIndex() == slot) {
                examsAtSlot.add(assignment);
            }
        }

        if (examsAtSlot.isEmpty()) {
            Label emptyLabel = new Label("-");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6;");
            cell.getChildren().add(emptyLabel);
        } else {
            // Sort by classroom
            examsAtSlot.sort(Comparator.comparing(ExamAssignment::getClassroomId));

            for (ExamAssignment exam : examsAtSlot) {
                HBox examBox = new HBox(5);
                examBox.setAlignment(Pos.CENTER_LEFT);
                examBox.setStyle("-fx-background-color: #3498db; -fx-background-radius: 3; -fx-padding: 3 6;");
                examBox.setCursor(Cursor.MOVE);

                Label courseLabel = new Label(exam.getCourseCode());
                courseLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");

                Label roomLabel = new Label("(" + exam.getClassroomId() + ")");
                roomLabel.setStyle("-fx-text-fill: #d4e6f1; -fx-font-size: 10;");

                examBox.getChildren().addAll(courseLabel, roomLabel);

                // Hover highlight
                examBox.setOnMouseEntered(e -> {
                    if (draggedExam == null) {
                        examBox.setStyle("-fx-background-color: #2980b9; -fx-background-radius: 3; -fx-padding: 3 6;");
                    }
                });
                examBox.setOnMouseExited(e -> {
                    if (draggedExam == null) {
                        examBox.setStyle("-fx-background-color: #3498db; -fx-background-radius: 3; -fx-padding: 3 6;");
                    }
                });

                // Click opens the edit dialog (which also shows details)
                examBox.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 && draggedExam == null) {
                        openEditDialog(exam);
                    }
                });

                // DRAG SOURCE - Start drag on exam box
                examBox.setOnDragDetected(e -> {
                    draggedExam = exam;
                    Dragboard db = examBox.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.put(EXAM_DATA_FORMAT, exam.getCourseCode());
                    db.setContent(content);

                    // Visual feedback - make the dragged box semi-transparent
                    examBox.setOpacity(0.5);

                    e.consume();
                });

                examBox.setOnDragDone(e -> {
                    draggedExam = null;
                    examBox.setOpacity(1.0);
                    // Refresh grid to reset all cell colors
                    if (currentSchedule != null && currentConfig != null) {
                        displayScheduleGrid(currentSchedule, currentConfig);
                    }
                    e.consume();
                });

                // Tooltip with hint
                Tooltip tooltip = new Tooltip(
                        exam.getCourseCode() + "\n" + exam.getClassroomId() + "\nDrag to move, click to edit");
                Tooltip.install(examBox, tooltip);

                cell.getChildren().add(examBox);
            }

            // Change cell color based on load
            if (examsAtSlot.size() >= 3) {
                cell.setStyle("-fx-background-color: #fadbd8; -fx-border-color: #e74c3c; -fx-border-width: 1;");
            } else if (examsAtSlot.size() >= 2) {
                cell.setStyle("-fx-background-color: #fcf3cf; -fx-border-color: #f39c12; -fx-border-width: 1;");
            }
        }

        // DROP TARGET - Set up cell as drop target
        setupDropTarget(cell, day, slot);

        return cell;
    }

    /**
     * Sets up a cell as a drop target for drag-and-drop operations.
     */
    private void setupDropTarget(VBox cell, int targetDay, int targetSlot) {
        // Accept drag over this cell
        cell.setOnDragOver(e -> {
            if (e.getGestureSource() != cell && draggedExam != null && e.getDragboard().hasContent(EXAM_DATA_FORMAT)) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        // Visual feedback when dragging over
        cell.setOnDragEntered(e -> {
            if (e.getGestureSource() != cell && draggedExam != null && e.getDragboard().hasContent(EXAM_DATA_FORMAT)) {
                // Validate if this drop would be valid
                String classroomId = draggedExam.getClassroomId();
                ConstraintValidationService.ValidationResult result = validationService.validateAssignment(
                        draggedExam.getCourseCode(),
                        targetDay, targetSlot, classroomId, currentSchedule);

                if (result.isValid()) {
                    // Valid drop zone - green
                    cell.setStyle("-fx-background-color: #d5f5e3; -fx-border-color: #27ae60; -fx-border-width: 2;");
                } else {
                    // Invalid drop zone - red
                    cell.setStyle("-fx-background-color: #fadbd8; -fx-border-color: #e74c3c; -fx-border-width: 2;");
                }
            }
            e.consume();
        });

        // Reset style when drag exits
        cell.setOnDragExited(e -> {
            // Reset to default style
            cell.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
            e.consume();
        });

        // Handle the drop
        cell.setOnDragDropped(e -> {
            boolean success = false;

            if (draggedExam != null && e.getDragboard().hasContent(EXAM_DATA_FORMAT)) {
                String classroomId = draggedExam.getClassroomId();

                // Validate the drop
                ConstraintValidationService.ValidationResult result = validationService.validateAssignment(
                        draggedExam.getCourseCode(),
                        targetDay, targetSlot, classroomId, currentSchedule);

                if (result.isValid()) {
                    // Perform the move
                    success = performExamMove(draggedExam.getCourseCode(), targetDay, targetSlot, classroomId, false);
                } else {
                    // Show confirmation for invalid drop
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Constraint Violation");
                    confirm.setHeaderText("Move would violate constraints");
                    confirm.setContentText(result.getFormattedMessage() + "\n\nDo you want to move anyway?");
                    confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                    Optional<ButtonType> confirmResult = confirm.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.YES) {
                        success = performExamMove(draggedExam.getCourseCode(), targetDay, targetSlot, classroomId,
                                true);
                    }
                }
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    /**
     * Performs an exam move operation and refreshes the display.
     */
    private boolean performExamMove(String courseCode, int newDay, int newSlot, String classroomId, boolean forced) {
        // Update the ScheduleState
        boolean updated = currentSchedule.updateAssignment(courseCode, newDay, newSlot, classroomId);

        if (updated) {
            // Also update the Course in DataManager
            Course course = dataManager.getCourse(courseCode);
            if (course != null) {
                course.setExamSchedule(newDay, newSlot, classroomId);
            }

            // Update status
            String message = forced ? "⚠️ Exam moved (with override)" : "✓ Exam moved successfully";
            statusLabel.setText(message);
            statusLabel.setStyle(forced ? "-fx-text-fill: #f39c12;" : "-fx-text-fill: #27ae60;");

            // Log the change
            System.out.println("Exam moved: " + courseCode + " -> Day " + (newDay + 1) +
                    ", Slot " + (newSlot + 1) + ", Room " + classroomId +
                    (forced ? " (forced)" : ""));

            return true;
        }
        return false;
    }

    @FXML
    private void onCancelGeneration() {
        if (generatorService != null) {
            generatorService.cancel();
        }

        cancelButton.setDisable(true);
        progressLabel.setText("Cancelling...");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Opens the edit dialog for an exam assignment.
     */
    private void openEditDialog(ExamAssignment exam) {
        if (currentSchedule == null || currentConfig == null) {
            showAlert(Alert.AlertType.WARNING, "Cannot Edit",
                    "No schedule is currently loaded. Please generate a schedule first.");
            return;
        }

        ExamEditDialog dialog = new ExamEditDialog(exam, currentSchedule, currentConfig);
        dialog.showAndWait().ifPresent(result -> {
            applyExamEdit(result);
        });
    }

    /**
     * Applies an edit result to the schedule.
     */
    private void applyExamEdit(ExamEditDialog.EditResult result) {
        if (result == null)
            return;

        String courseCode = result.getCourseCode();
        int newDay = result.getDay();
        int newSlot = result.getTimeSlot();
        String newClassroom = result.getClassroomId();

        // Update the ScheduleState
        boolean updated = currentSchedule.updateAssignment(courseCode, newDay, newSlot, newClassroom);

        if (updated) {
            // Also update the Course in DataManager
            Course course = dataManager.getCourse(courseCode);
            if (course != null) {
                course.setExamSchedule(newDay, newSlot, newClassroom);
            }

            // Refresh the grid display
            displayScheduleGrid(currentSchedule, currentConfig);

            // Update status
            String message = result.isForcedOverride() ? "⚠️ Exam moved (with override)" : "✓ Exam moved successfully";
            statusLabel.setText(message);
            statusLabel.setStyle(result.isForcedOverride() ? "-fx-text-fill: #f39c12;" : "-fx-text-fill: #27ae60;");

            // Log the change
            System.out.println("Exam edited: " + courseCode + " -> Day " + (newDay + 1) +
                    ", Slot " + (newSlot + 1) + ", Room " + newClassroom +
                    (result.isForcedOverride() ? " (forced)" : ""));
        } else {
            showAlert(Alert.AlertType.ERROR, "Edit Failed",
                    "Could not update the exam assignment. The exam may be locked.");
        }
    }

    /**
     * Exports the schedule as a CSV grid (days × time slots).
     */
    @FXML
    private void onExportCSV() {
        if (currentSchedule == null || currentConfig == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule",
                    "Please generate a schedule first.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Schedule as CSV");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("schedule_calendar.csv");

        java.io.File file = fileChooser.showSaveDialog(scheduleGrid.getScene().getWindow());
        if (file == null)
            return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            int numDays = currentConfig.getNumDays();
            int slotsPerDay = currentConfig.getSlotsPerDay();

            // Header row: Time Slot, Day 1, Day 2, ...
            StringBuilder header = new StringBuilder("Time Slot");
            for (int day = 0; day < numDays; day++) {
                LocalDate date = currentConfig.getStartDate().plusDays(day);
                header.append(",").append(date.toString());
            }
            writer.println(header);

            // Data rows: one per time slot
            for (int slot = 0; slot < slotsPerDay; slot++) {
                StringBuilder row = new StringBuilder();
                TimeSlot timeSlot = currentConfig.getTimeSlot(0, slot);
                String slotLabel = timeSlot != null
                        ? timeSlot.getStartTime() + "-" + timeSlot.getEndTime()
                        : "Slot " + (slot + 1);
                row.append(slotLabel);

                for (int day = 0; day < numDays; day++) {
                    row.append(",");
                    // Find exams at this day/slot
                    List<String> exams = new ArrayList<>();
                    for (ExamAssignment a : currentSchedule.getAssignments().values()) {
                        if (a.isAssigned() && a.getDay() == day && a.getTimeSlotIndex() == slot) {
                            exams.add(a.getCourseCode() + " (" + a.getClassroomId() + ")");
                        }
                    }
                    row.append("\"").append(String.join("; ", exams)).append("\"");
                }
                writer.println(row);
            }

            showAlert(Alert.AlertType.INFORMATION, "Export Complete",
                    "Schedule exported to:\n" + file.getAbsolutePath());
        } catch (java.io.IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not write file: " + e.getMessage());
        }
    }
}
