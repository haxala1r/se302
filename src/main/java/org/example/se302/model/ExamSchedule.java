package org.example.se302.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Container class that holds the complete exam schedule.
 * This is the main class that manages all exam assignments and provides
 * methods for accessing, modifying, and validating the schedule.
 * 
 * <h3>Structure:</h3>
 * 
 * <pre>
 * ExamSchedule = {
 *     configuration: ScheduleConfiguration,
 *     assignments: Map<CourseCode, ExamAssignment>,
 *     statistics: ScheduleStatistics
 * }
 * </pre>
 */
public class ExamSchedule {

    // Configuration for this schedule
    private ScheduleConfiguration configuration;

    // All assignments indexed by course code
    private Map<String, ExamAssignment> assignments;

    // Schedule organized by day and slot for quick access
    // Key: "D{day}_S{slot}" -> List of assignments at that time
    private Map<String, List<ExamAssignment>> scheduleGrid;

    // Classroom usage tracking
    // Key: "D{day}_S{slot}_R{roomId}" -> assignment (if any)
    private Map<String, ExamAssignment> classroomUsage;

    // Metadata
    private String scheduleName;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private boolean isFinalized;

    // Statistics
    private int totalCourses;
    private int scheduledCourses;
    private int constraintViolations;

    /**
     * Creates an empty exam schedule.
     */
    public ExamSchedule() {
        this.configuration = new ScheduleConfiguration();
        this.assignments = new LinkedHashMap<>(); // Preserve insertion order
        this.scheduleGrid = new TreeMap<>();
        this.classroomUsage = new HashMap<>();
        this.scheduleName = "New Schedule";
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.isFinalized = false;
        this.totalCourses = 0;
        this.scheduledCourses = 0;
        this.constraintViolations = 0;
    }

    /**
     * Creates an exam schedule with the given configuration.
     */
    public ExamSchedule(ScheduleConfiguration configuration) {
        this();
        this.configuration = configuration;
    }

    /**
     * Creates an exam schedule from a ScheduleState.
     */
    public ExamSchedule(ScheduleState state) {
        this();
        if (state.getConfiguration() != null) {
            this.configuration = state.getConfiguration();
        }

        // Import all assignments from state
        for (ExamAssignment assignment : state.getAssignments().values()) {
            addAssignment(assignment.copy());
        }
    }

    // ==================== Assignment Management ====================

    /**
     * Adds a new course to the schedule (unassigned initially).
     */
    public void addCourse(String courseCode, int studentCount) {
        if (!assignments.containsKey(courseCode)) {
            ExamAssignment assignment = new ExamAssignment(courseCode);
            assignment.setStudentCount(studentCount);
            assignments.put(courseCode, assignment);
            totalCourses++;
            lastModified = LocalDateTime.now();
        }
    }

    /**
     * Adds an exam assignment to the schedule.
     */
    public void addAssignment(ExamAssignment assignment) {
        String courseCode = assignment.getCourseCode();

        // Remove old assignment if exists
        if (assignments.containsKey(courseCode)) {
            removeFromGrid(assignments.get(courseCode));
        } else {
            totalCourses++;
        }

        assignments.put(courseCode, assignment);

        if (assignment.isAssigned()) {
            addToGrid(assignment);
            scheduledCourses++;
        }

        lastModified = LocalDateTime.now();
    }

    /**
     * Schedules a course at the specified day, slot, and classroom.
     * 
     * @return true if successful, false if there's a conflict
     */
    public boolean scheduleCourse(String courseCode, int day, int timeSlot, String classroomId) {
        ExamAssignment assignment = assignments.get(courseCode);
        if (assignment == null) {
            return false;
        }

        if (assignment.isLocked()) {
            return false;
        }

        // Check if classroom is available
        String usageKey = "D" + day + "_S" + timeSlot + "_" + classroomId;
        if (classroomUsage.containsKey(usageKey)) {
            return false; // Classroom conflict
        }

        // Remove from old position if was scheduled
        if (assignment.isAssigned()) {
            removeFromGrid(assignment);
            scheduledCourses--;
        }

        // Update assignment
        assignment.setDay(day);
        assignment.setTimeSlotIndex(timeSlot);
        assignment.setClassroomId(classroomId);

        // Set TimeSlot object if configuration available
        if (configuration != null) {
            assignment.setTimeSlot(configuration.getTimeSlot(day, timeSlot));
        }

        // Add to grid
        addToGrid(assignment);
        scheduledCourses++;
        lastModified = LocalDateTime.now();

        return true;
    }

    /**
     * Unschedules a course (removes its assignment).
     */
    public boolean unscheduleCourse(String courseCode) {
        ExamAssignment assignment = assignments.get(courseCode);
        if (assignment == null || assignment.isLocked()) {
            return false;
        }

        if (assignment.isAssigned()) {
            removeFromGrid(assignment);
            scheduledCourses--;
        }

        assignment.setDay(-1);
        assignment.setTimeSlotIndex(-1);
        assignment.setClassroomId(null);
        assignment.setTimeSlot(null);

        lastModified = LocalDateTime.now();
        return true;
    }

    /**
     * Adds an assignment to the schedule grid.
     */
    private void addToGrid(ExamAssignment assignment) {
        if (!assignment.isAssigned())
            return;

        String timeKey = assignment.getTimeKey();
        scheduleGrid.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(assignment);

        String usageKey = assignment.getFullKey();
        classroomUsage.put(usageKey, assignment);
    }

    /**
     * Removes an assignment from the schedule grid.
     */
    private void removeFromGrid(ExamAssignment assignment) {
        if (!assignment.isAssigned())
            return;

        String timeKey = assignment.getTimeKey();
        List<ExamAssignment> slotAssignments = scheduleGrid.get(timeKey);
        if (slotAssignments != null) {
            slotAssignments.removeIf(a -> a.getCourseCode().equals(assignment.getCourseCode()));
            if (slotAssignments.isEmpty()) {
                scheduleGrid.remove(timeKey);
            }
        }

        String usageKey = assignment.getFullKey();
        classroomUsage.remove(usageKey);
    }

    // ==================== Query Methods ====================

    /**
     * Gets an assignment by course code.
     */
    public ExamAssignment getAssignment(String courseCode) {
        return assignments.get(courseCode);
    }

    /**
     * Gets all assignments.
     */
    public Collection<ExamAssignment> getAllAssignments() {
        return Collections.unmodifiableCollection(assignments.values());
    }

    /**
     * Gets all scheduled assignments.
     */
    public List<ExamAssignment> getScheduledAssignments() {
        List<ExamAssignment> scheduled = new ArrayList<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssigned()) {
                scheduled.add(assignment);
            }
        }
        return scheduled;
    }

    /**
     * Gets all unscheduled assignments.
     */
    public List<ExamAssignment> getUnscheduledAssignments() {
        List<ExamAssignment> unscheduled = new ArrayList<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (!assignment.isAssigned()) {
                unscheduled.add(assignment);
            }
        }
        return unscheduled;
    }

    /**
     * Gets all assignments at a specific day and slot.
     */
    public List<ExamAssignment> getAssignmentsAt(int day, int timeSlot) {
        String timeKey = "D" + day + "_S" + timeSlot;
        List<ExamAssignment> result = scheduleGrid.get(timeKey);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    /**
     * Gets the assignment at a specific day, slot, and classroom.
     */
    public ExamAssignment getAssignmentAt(int day, int timeSlot, String classroomId) {
        String usageKey = "D" + day + "_S" + timeSlot + "_" + classroomId;
        return classroomUsage.get(usageKey);
    }

    /**
     * Checks if a classroom is available at a specific day and slot.
     */
    public boolean isClassroomAvailable(String classroomId, int day, int timeSlot) {
        String usageKey = "D" + day + "_S" + timeSlot + "_" + classroomId;
        return !classroomUsage.containsKey(usageKey);
    }

    /**
     * Gets all assignments for a specific day.
     */
    public List<ExamAssignment> getAssignmentsForDay(int day) {
        List<ExamAssignment> dayAssignments = new ArrayList<>();
        for (int slot = 0; slot < configuration.getSlotsPerDay(); slot++) {
            dayAssignments.addAll(getAssignmentsAt(day, slot));
        }
        return dayAssignments;
    }

    // ==================== Validation Methods ====================

    /**
     * Checks if the schedule is complete (all courses scheduled).
     */
    public boolean isComplete() {
        return scheduledCourses == totalCourses;
    }

    /**
     * Gets the completion percentage.
     */
    public double getCompletionPercentage() {
        if (totalCourses == 0)
            return 100.0;
        return (scheduledCourses * 100.0) / totalCourses;
    }

    /**
     * Validates the entire schedule and returns a list of violations.
     */
    public List<String> validateSchedule(Map<String, Course> courses) {
        List<String> violations = new ArrayList<>();

        // Check for classroom conflicts (shouldn't happen with our data structure)
        // Check for student conflicts
        Map<String, List<ExamAssignment>> timeSlotAssignments = new HashMap<>();

        for (ExamAssignment assignment : assignments.values()) {
            if (!assignment.isAssigned())
                continue;

            String timeKey = assignment.getTimeKey();
            timeSlotAssignments.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(assignment);
        }

        // Check each time slot for student conflicts
        for (List<ExamAssignment> slotAssignments : timeSlotAssignments.values()) {
            for (int i = 0; i < slotAssignments.size(); i++) {
                for (int j = i + 1; j < slotAssignments.size(); j++) {
                    ExamAssignment a1 = slotAssignments.get(i);
                    ExamAssignment a2 = slotAssignments.get(j);

                    Course c1 = courses.get(a1.getCourseCode());
                    Course c2 = courses.get(a2.getCourseCode());

                    if (c1 != null && c2 != null) {
                        // Check for shared students
                        Set<String> students1 = new HashSet<>(c1.getEnrolledStudents());
                        for (String student : c2.getEnrolledStudents()) {
                            if (students1.contains(student)) {
                                violations.add(String.format(
                                        "Student conflict: %s and %s share students at Day %d Slot %d",
                                        a1.getCourseCode(), a2.getCourseCode(),
                                        a1.getDay() + 1, a1.getTimeSlotIndex() + 1));
                                break;
                            }
                        }
                    }
                }
            }
        }

        constraintViolations = violations.size();
        return violations;
    }

    // ==================== Export Methods ====================

    /**
     * Converts this schedule to a ScheduleState for use with CSP solver.
     */
    public ScheduleState toScheduleState() {
        ScheduleState state = new ScheduleState(configuration);

        for (ExamAssignment assignment : assignments.values()) {
            state.addAssignment(assignment.copy());
        }

        return state;
    }

    /**
     * Gets the schedule as a 2D grid [day][slot] for display.
     */
    public List<List<List<ExamAssignment>>> toGrid() {
        int numDays = configuration.getNumDays();
        int slotsPerDay = configuration.getSlotsPerDay();

        List<List<List<ExamAssignment>>> grid = new ArrayList<>();

        for (int day = 0; day < numDays; day++) {
            List<List<ExamAssignment>> daySlots = new ArrayList<>();
            for (int slot = 0; slot < slotsPerDay; slot++) {
                daySlots.add(getAssignmentsAt(day, slot));
            }
            grid.add(daySlots);
        }

        return grid;
    }

    // ==================== Getters and Setters ====================

    public ScheduleConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ScheduleConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public void setFinalized(boolean finalized) {
        isFinalized = finalized;
        lastModified = LocalDateTime.now();
    }

    public int getTotalCourses() {
        return totalCourses;
    }

    public int getScheduledCourses() {
        return scheduledCourses;
    }

    public int getUnscheduledCourses() {
        return totalCourses - scheduledCourses;
    }

    public int getConstraintViolations() {
        return constraintViolations;
    }

    @Override
    public String toString() {
        return String.format("ExamSchedule[%s, %d/%d scheduled (%.1f%%)]",
                scheduleName, scheduledCourses, totalCourses, getCompletionPercentage());
    }

    /**
     * Returns a detailed summary of the schedule.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("           EXAM SCHEDULE SUMMARY\n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append(String.format("Name: %s\n", scheduleName));
        sb.append(String.format("Status: %s\n", isFinalized ? "FINALIZED" : "DRAFT"));
        sb.append(String.format("Created: %s\n", createdAt));
        sb.append(String.format("Last Modified: %s\n", lastModified));
        sb.append("───────────────────────────────────────────\n");
        sb.append(String.format("Total Courses: %d\n", totalCourses));
        sb.append(String.format("Scheduled: %d (%.1f%%)\n", scheduledCourses, getCompletionPercentage()));
        sb.append(String.format("Unscheduled: %d\n", getUnscheduledCourses()));
        sb.append(String.format("Violations: %d\n", constraintViolations));
        sb.append("───────────────────────────────────────────\n");
        sb.append("Configuration:\n");
        sb.append(String.format("  Days: %d, Slots/Day: %d\n",
                configuration.getNumDays(), configuration.getSlotsPerDay()));
        sb.append(String.format("  Start Date: %s\n", configuration.getStartDate()));
        sb.append("═══════════════════════════════════════════\n");

        return sb.toString();
    }

    /**
     * Prints the schedule in a grid format.
     */
    public String toGridString() {
        StringBuilder sb = new StringBuilder();
        int numDays = configuration.getNumDays();
        int slotsPerDay = configuration.getSlotsPerDay();

        sb.append("\n┌───────────┬");
        for (int slot = 0; slot < slotsPerDay; slot++) {
            sb.append("─────────────────────────┬");
        }
        sb.setLength(sb.length() - 1);
        sb.append("┐\n");

        sb.append("│           │");
        for (int slot = 0; slot < slotsPerDay; slot++) {
            sb.append(String.format(" Slot %-18d │", slot + 1));
        }
        sb.setLength(sb.length() - 1);
        sb.append("│\n");

        for (int day = 0; day < numDays; day++) {
            sb.append("├───────────┼");
            for (int slot = 0; slot < slotsPerDay; slot++) {
                sb.append("─────────────────────────┼");
            }
            sb.setLength(sb.length() - 1);
            sb.append("┤\n");

            sb.append(String.format("│ Day %-5d │", day + 1));
            for (int slot = 0; slot < slotsPerDay; slot++) {
                List<ExamAssignment> slotAssignments = getAssignmentsAt(day, slot);
                if (slotAssignments.isEmpty()) {
                    sb.append("                         │");
                } else {
                    String display = slotAssignments.size() + " exams";
                    sb.append(String.format(" %-23s │", display));
                }
            }
            sb.setLength(sb.length() - 1);
            sb.append("│\n");
        }

        sb.append("└───────────┴");
        for (int slot = 0; slot < slotsPerDay; slot++) {
            sb.append("─────────────────────────┴");
        }
        sb.setLength(sb.length() - 1);
        sb.append("┘\n");

        return sb.toString();
    }
}
