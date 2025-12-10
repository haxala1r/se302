package org.example.se302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.example.se302.model.ImportResult;
import org.example.se302.service.DataImportService;
import org.example.se302.service.DataManager;

import java.io.File;

/**
 * Controller for the Import Data view.
 */
public class ImportController {

    @FXML private TextField studentFileField;
    @FXML private TextField courseFileField;
    @FXML private TextField classroomFileField;
    @FXML private TextField enrollmentFileField;

    @FXML private Label studentStatusLabel;
    @FXML private Label courseStatusLabel;
    @FXML private Label classroomStatusLabel;
    @FXML private Label enrollmentStatusLabel;

    @FXML private TextArea messagesArea;
    @FXML private Button importAllButton;

    private File studentFile;
    private File courseFile;
    private File classroomFile;
    private File enrollmentFile;

    private DataImportService importService;
    private DataManager dataManager;

    @FXML
    public void initialize() {
        importService = new DataImportService();
        dataManager = DataManager.getInstance();
    }

    @FXML
    private void onBrowseStudents() {
        FileChooser fileChooser = createFileChooser("Select Student Data CSV");
        studentFile = fileChooser.showOpenDialog(studentFileField.getScene().getWindow());

        if (studentFile != null) {
            studentFileField.setText(studentFile.getAbsolutePath());
            studentStatusLabel.setText("File selected - Ready to import");
            studentStatusLabel.setStyle("-fx-text-fill: blue;");
            checkAllFilesSelected();
        }
    }

    @FXML
    private void onBrowseCourses() {
        FileChooser fileChooser = createFileChooser("Select Course Data CSV");
        courseFile = fileChooser.showOpenDialog(courseFileField.getScene().getWindow());

        if (courseFile != null) {
            courseFileField.setText(courseFile.getAbsolutePath());
            courseStatusLabel.setText("File selected - Ready to import");
            courseStatusLabel.setStyle("-fx-text-fill: blue;");
            checkAllFilesSelected();
        }
    }

    @FXML
    private void onBrowseClassrooms() {
        FileChooser fileChooser = createFileChooser("Select Classroom Data CSV");
        classroomFile = fileChooser.showOpenDialog(classroomFileField.getScene().getWindow());

        if (classroomFile != null) {
            classroomFileField.setText(classroomFile.getAbsolutePath());
            classroomStatusLabel.setText("File selected - Ready to import");
            classroomStatusLabel.setStyle("-fx-text-fill: blue;");
            checkAllFilesSelected();
        }
    }

    @FXML
    private void onBrowseEnrollments() {
        FileChooser fileChooser = createFileChooser("Select Enrollment Data CSV");
        enrollmentFile = fileChooser.showOpenDialog(enrollmentFileField.getScene().getWindow());

        if (enrollmentFile != null) {
            enrollmentFileField.setText(enrollmentFile.getAbsolutePath());
            enrollmentStatusLabel.setText("File selected - Ready to import");
            enrollmentStatusLabel.setStyle("-fx-text-fill: blue;");
            checkAllFilesSelected();
        }
    }

    @FXML
    private void onImportAll() {
        messagesArea.clear();
        messagesArea.appendText("Starting import process...\n\n");

        // Clear existing data
        dataManager.clearAll();

        // Import students
        messagesArea.appendText("Importing students...\n");
        ImportResult studentResult = importService.importStudents(studentFile);
        updateStatus(studentStatusLabel, studentResult);
        messagesArea.appendText(studentResult.getFormattedMessage() + "\n");

        // Import courses
        messagesArea.appendText("Importing courses...\n");
        ImportResult courseResult = importService.importCourses(courseFile);
        updateStatus(courseStatusLabel, courseResult);
        messagesArea.appendText(courseResult.getFormattedMessage() + "\n");

        // Import classrooms
        messagesArea.appendText("Importing classrooms...\n");
        ImportResult classroomResult = importService.importClassrooms(classroomFile);
        updateStatus(classroomStatusLabel, classroomResult);
        messagesArea.appendText(classroomResult.getFormattedMessage() + "\n");

        // Import enrollments (must be after students and courses)
        messagesArea.appendText("Importing enrollments...\n");
        ImportResult enrollmentResult = importService.importEnrollments(enrollmentFile);
        updateStatus(enrollmentStatusLabel, enrollmentResult);
        messagesArea.appendText(enrollmentResult.getFormattedMessage() + "\n");

        // Check if all imports were successful
        if (studentResult.isSuccess() && courseResult.isSuccess() &&
            classroomResult.isSuccess() && enrollmentResult.isSuccess()) {

            messagesArea.appendText("\n========================================\n");
            messagesArea.appendText("IMPORT SUCCESSFUL!\n");
            messagesArea.appendText("========================================\n");
            messagesArea.appendText(String.format("- Loaded %d students\n", dataManager.getTotalStudents()));
            messagesArea.appendText(String.format("- Loaded %d courses\n", dataManager.getTotalCourses()));
            messagesArea.appendText(String.format("- Loaded %d classrooms\n", dataManager.getTotalClassrooms()));
            messagesArea.appendText("- Data is ready for viewing and scheduling\n");
            messagesArea.appendText("\nYou can now navigate to other tabs to view the data.\n");

            // Enable other tabs
            enableDataTabs();
        } else {
            messagesArea.appendText("\n========================================\n");
            messagesArea.appendText("IMPORT COMPLETED WITH ERRORS\n");
            messagesArea.appendText("========================================\n");
            messagesArea.appendText("Please check the error messages above and fix the CSV files.\n");
        }
    }

    @FXML
    private void onClearAll() {
        studentFile = null;
        courseFile = null;
        classroomFile = null;
        enrollmentFile = null;

        studentFileField.clear();
        courseFileField.clear();
        classroomFileField.clear();
        enrollmentFileField.clear();

        studentStatusLabel.setText("Not Loaded");
        courseStatusLabel.setText("Not Loaded");
        classroomStatusLabel.setText("Not Loaded");
        enrollmentStatusLabel.setText("Not Loaded");

        studentStatusLabel.setStyle("");
        courseStatusLabel.setStyle("");
        classroomStatusLabel.setStyle("");
        enrollmentStatusLabel.setStyle("");

        messagesArea.clear();
        importAllButton.setDisable(true);

        dataManager.clearAll();
    }

    private FileChooser createFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        // Set initial directory to sampleData if it exists
        File sampleDataDir = new File("sampleData");
        if (sampleDataDir.exists() && sampleDataDir.isDirectory()) {
            fileChooser.setInitialDirectory(sampleDataDir);
        }

        return fileChooser;
    }

    private void checkAllFilesSelected() {
        boolean allSelected = studentFile != null && courseFile != null &&
                             classroomFile != null && enrollmentFile != null;
        importAllButton.setDisable(!allSelected);
    }

    private void updateStatus(Label statusLabel, ImportResult result) {
        if (result.isSuccess()) {
            statusLabel.setText("Loaded: " + result.getRecordCount() + " records");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("Error - See messages below");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void enableDataTabs() {
        // The tabs will be enabled via the MainController
        // For now, we'll use a simple approach by navigating up the scene graph
        try {
            // Get the root BorderPane from main-view.fxml
            javafx.scene.Parent root = studentFileField.getScene().getRoot();

            // Since we can't easily access the MainController from here,
            // we'll just let the user know they can navigate to other tabs
            // The tabs are already set to enabled in the success message
            messagesArea.appendText("Note: You can now switch to the Students, Courses, Classrooms, or Schedule Views tabs.\n");
        } catch (Exception e) {
            // Ignore if we can't access the scene graph
        }
    }
}
