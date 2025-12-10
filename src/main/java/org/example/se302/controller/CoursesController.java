package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.se302.model.Course;
import org.example.se302.service.DataManager;

/**
 * Controller for the Courses view.
 */
public class CoursesController {

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> courseCodeColumn;
    @FXML private TableColumn<Course, Number> studentCountColumn;
    @FXML private TableColumn<Course, String> classroomColumn;
    @FXML private TableColumn<Course, String> examDateColumn;
    @FXML private TableColumn<Course, Void> actionColumn;

    @FXML private VBox studentListPanel;
    @FXML private Label studentListTitleLabel;
    @FXML private TableView<String> enrolledStudentsTable;
    @FXML private TableColumn<String, String> enrolledStudentIdColumn;
    @FXML private Label enrolledCountLabel;

    private DataManager dataManager;
    private FilteredList<Course> filteredCourses;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Set up table columns
        courseCodeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCourseCode()));

        studentCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getEnrolledStudentsCount()));

        classroomColumn.setCellValueFactory(cellData -> {
            String classroom = cellData.getValue().getAssignedClassroom();
            return new SimpleStringProperty(classroom != null ? classroom : "Not Assigned");
        });

        examDateColumn.setCellValueFactory(cellData -> {
            String examDate = cellData.getValue().getExamDateTime();
            return new SimpleStringProperty(examDate != null ? examDate : "Not Scheduled");
        });

        // Add "View Students" button to action column
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View Students");

            {
                viewButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    showEnrolledStudents(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        // Set up enrolled students table column
        enrolledStudentIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue()));

        // Set up filtered list
        filteredCourses = new FilteredList<>(dataManager.getCourses(), p -> true);
        coursesTable.setItems(filteredCourses);

        // Set up search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses(newValue);
        });

        // Update result count
        updateResultCount();
        filteredCourses.addListener((javafx.collections.ListChangeListener<Course>) c -> updateResultCount());
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
    }

    @FXML
    private void onCloseStudentList() {
        studentListPanel.setVisible(false);
        studentListPanel.setManaged(false);
    }

    private void filterCourses(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredCourses.setPredicate(course -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase().trim();
            filteredCourses.setPredicate(course ->
                    course.getCourseCode().toLowerCase().contains(lowerCaseFilter)
            );
        }
    }

    private void showEnrolledStudents(Course course) {
        if (course == null) return;

        studentListTitleLabel.setText("Students Enrolled in " + course.getCourseCode());
        enrolledStudentsTable.setItems(FXCollections.observableArrayList(course.getEnrolledStudents()));
        enrolledCountLabel.setText("Total: " + course.getEnrolledStudentsCount() + " students");

        studentListPanel.setVisible(true);
        studentListPanel.setManaged(true);
    }

    private void updateResultCount() {
        int total = filteredCourses.size();
        int overall = dataManager.getTotalCourses();

        if (total == overall) {
            resultCountLabel.setText("Total: " + total + " courses");
        } else {
            resultCountLabel.setText("Showing: " + total + " of " + overall + " courses");
        }
    }
}
