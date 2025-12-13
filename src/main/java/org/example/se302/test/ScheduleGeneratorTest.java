package org.example.se302.test;

import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleConfiguration;
import org.example.se302.service.DataImportService;
import org.example.se302.service.DataManager;
import org.example.se302.service.ScheduleGeneratorService;

import java.io.File;

/**
 * Simple test to verify schedule generation works with sample data.
 */
public class ScheduleGeneratorTest {

    public static void main(String[] args) {
        System.out.println("=== Schedule Generator Test ===\n");

        // Step 1: Import sample data
        System.out.println("Step 1: Importing sample data...");
        DataManager dataManager = DataManager.getInstance();
        DataImportService importService = new DataImportService();

        try {
            importService.importStudents(new File("sampleData/sampleData_AllStudents.csv"));
            System.out.println("  ✓ Imported " + dataManager.getTotalStudents() + " students");

            importService.importCourses(new File("sampleData/sampleData_AllCourses.csv"));
            System.out.println("  ✓ Imported " + dataManager.getTotalCourses() + " courses");

            importService.importClassrooms(new File("sampleData/sampleData_AllClassroomsAndTheirCapacities.csv"));
            System.out.println("  ✓ Imported " + dataManager.getTotalClassrooms() + " classrooms");

            importService.importEnrollments(new File("sampleData/sampleData_AllAttendanceLists.csv"));
            System.out.println("  ✓ Imported enrollment data\n");

        } catch (Exception e) {
            System.err.println("Error importing data: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Step 2: Create schedule configuration
        System.out.println("Step 2: Creating schedule configuration...");
        ScheduleConfiguration config = new ScheduleConfiguration(
            5,  // 5 days
            4,  // 4 slots per day
            ScheduleConfiguration.OptimizationStrategy.DEFAULT
        );
        System.out.println("  ✓ Configuration: " + config.getNumDays() + " days, "
                         + config.getSlotsPerDay() + " slots/day = "
                         + config.getTotalSlots() + " total slots\n");

        // Step 3: Generate schedule
        System.out.println("Step 3: Generating schedule...");
        ScheduleGeneratorService generator = new ScheduleGeneratorService();

        // Add progress listener
        generator.setProgressListener((progress, message) -> {
            System.out.printf("  [%.0f%%] %s\n", progress * 100, message);
        });

        ScheduleGeneratorService.ScheduleResult result = generator.generateSchedule(config);

        // Step 4: Display results
        System.out.println("\n=== RESULT ===");
        if (result.isSuccess()) {
            System.out.println("✓ SUCCESS: Schedule generated!");
            System.out.println("Message: " + result.getMessage());

            // Show some assignments
            System.out.println("\nSample assignments:");
            int count = 0;
            for (ExamAssignment assignment : result.getScheduleState().getAssignments().values()) {
                if (assignment.isAssigned() && count < 5) {
                    System.out.println("  " + assignment.toDisplayString());
                    count++;
                }
            }
            if (result.getScheduleState().getAssignments().size() > 5) {
                System.out.println("  ... and " +
                    (result.getScheduleState().getAssignments().size() - 5) + " more");
            }

        } else if (result.wasCancelled()) {
            System.out.println("⚠ CANCELLED: " + result.getMessage());
        } else {
            System.out.println("✗ FAILED: " + result.getMessage());
            System.out.println("\nPossible solutions:");
            System.out.println("  - Increase number of days");
            System.out.println("  - Increase slots per day");
            System.out.println("  - Add more classrooms");
            System.out.println("  - Relax constraints");
        }
    }
}
