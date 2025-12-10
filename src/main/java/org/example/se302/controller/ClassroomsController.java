package org.example.se302.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.se302.model.Classroom;
import org.example.se302.service.DataManager;

/**
 * Controller for the Classrooms view.
 */
public class ClassroomsController {

    @FXML private TableView<Classroom> classroomsTable;
    @FXML private TableColumn<Classroom, String> classroomIdColumn;
    @FXML private TableColumn<Classroom, Number> capacityColumn;
    @FXML private TableColumn<Classroom, String> statusColumn;
    @FXML private TableColumn<Classroom, String> utilizationColumn;

    @FXML private Label totalClassroomsLabel;
    @FXML private Label totalCapacityLabel;
    @FXML private Label averageCapacityLabel;

    private DataManager dataManager;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Set up table columns
        classroomIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClassroomId()));

        capacityColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getCapacity()));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("Available")); // For demo phase

        utilizationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("Not Scheduled")); // For demo phase

        // Bind table to data
        classroomsTable.setItems(dataManager.getClassrooms());

        // Update summary statistics
        updateSummaryStatistics();

        // Listen for data changes
        dataManager.getClassrooms().addListener(
                (javafx.collections.ListChangeListener<Classroom>) c -> updateSummaryStatistics());
    }

    private void updateSummaryStatistics() {
        int totalClassrooms = dataManager.getTotalClassrooms();
        int totalCapacity = dataManager.getClassrooms().stream()
                .mapToInt(Classroom::getCapacity)
                .sum();
        double averageCapacity = totalClassrooms > 0 ?
                (double) totalCapacity / totalClassrooms : 0;

        totalClassroomsLabel.setText("Total Classrooms: " + totalClassrooms);
        totalCapacityLabel.setText("Total Capacity: " + totalCapacity);
        averageCapacityLabel.setText(String.format("Average Capacity: %.1f", averageCapacity));
    }
}
