package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.se302.model.Student;
import org.example.se302.service.DataManager;

/**
 * Controller for the Students view.
 */
public class StudentsController {

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> studentIdColumn;
    @FXML private TableColumn<Student, Number> courseCountColumn;
    @FXML private TableColumn<Student, Void> actionColumn;

    @FXML private VBox detailPanel;
    @FXML private Label detailStudentIdLabel;
    @FXML private ListView<String> coursesList;
    @FXML private Label detailCourseCountLabel;

    private DataManager dataManager;
    private FilteredList<Student> filteredStudents;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Set up table columns
        studentIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStudentId()));

        courseCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getEnrolledCoursesCount()));

        // Add "View Details" button to action column
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View Details");

            {
                viewButton.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    showStudentDetails(student);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        // Set up filtered list
        filteredStudents = new FilteredList<>(dataManager.getStudents(), p -> true);
        studentsTable.setItems(filteredStudents);

        // Set up search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterStudents(newValue);
        });

        // Update result count
        updateResultCount();
        filteredStudents.addListener((javafx.collections.ListChangeListener<Student>) c -> updateResultCount());
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
    }

    @FXML
    private void onCloseDetail() {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    private void filterStudents(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredStudents.setPredicate(student -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase().trim();
            filteredStudents.setPredicate(student ->
                    student.getStudentId().toLowerCase().contains(lowerCaseFilter)
            );
        }
    }

    private void showStudentDetails(Student student) {
        if (student == null) return;

        detailStudentIdLabel.setText("Student ID: " + student.getStudentId());
        coursesList.setItems(FXCollections.observableArrayList(student.getEnrolledCourses()));
        detailCourseCountLabel.setText("Total: " + student.getEnrolledCoursesCount() + " courses");

        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    private void updateResultCount() {
        int total = filteredStudents.size();
        int overall = dataManager.getTotalStudents();

        if (total == overall) {
            resultCountLabel.setText("Total: " + total + " students");
        } else {
            resultCountLabel.setText("Showing: " + total + " of " + overall + " students");
        }
    }
}
