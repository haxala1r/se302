package org.example.se302.model;

import java.util.*;

/**
 * Represents the complete state of an exam schedule.
 * This is the main data structure used by the CSP solver to track
 * the current schedule state and validate constraints.
 * 
 * Supports both TimeSlot-based and day/timeSlotIndex-based assignments.
 */
public class ScheduleState {
    // All exam assignments indexed by course code
    private Map<String, ExamAssignment> assignments;

    // Quick lookup structures for constraint checking
    private Map<String, Set<String>> classroomTimeSlotUsage; // classroomId -> set of timeSlot keys
    private Map<String, Set<String>> timeSlotCourseMapping; // timeSlotKey -> set of course codes

    // Available resources
    private List<TimeSlot> availableTimeSlots;
    private List<Classroom> availableClassrooms;

    // Configuration reference
    private ScheduleConfiguration configuration;

    // Statistics
    private int totalCourses;
    private int assignedCourses;
    private int constraintViolations;

    public ScheduleState() {
        this.assignments = new HashMap<>();
        this.classroomTimeSlotUsage = new HashMap<>();
        this.timeSlotCourseMapping = new HashMap<>();
        this.availableTimeSlots = new ArrayList<>();
        this.availableClassrooms = new ArrayList<>();
        this.configuration = null;
        this.totalCourses = 0;
        this.assignedCourses = 0;
        this.constraintViolations = 0;
    }

    /**
     * Creates a ScheduleState with configuration.
     */
    public ScheduleState(ScheduleConfiguration configuration) {
        this();
        this.configuration = configuration;
        if (configuration != null) {
            this.availableTimeSlots = configuration.generateTimeSlots();
        }
    }

    /**
     * Creates a deep copy of this schedule state.
     */
    public ScheduleState copy() {
        ScheduleState copy = new ScheduleState();

        for (Map.Entry<String, ExamAssignment> entry : this.assignments.entrySet()) {
            copy.assignments.put(entry.getKey(), entry.getValue().copy());
        }

        for (Map.Entry<String, Set<String>> entry : this.classroomTimeSlotUsage.entrySet()) {
            copy.classroomTimeSlotUsage.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        for (Map.Entry<String, Set<String>> entry : this.timeSlotCourseMapping.entrySet()) {
            copy.timeSlotCourseMapping.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        copy.availableTimeSlots = new ArrayList<>(this.availableTimeSlots);
        copy.availableClassrooms = new ArrayList<>(this.availableClassrooms);
        copy.configuration = this.configuration;
        copy.totalCourses = this.totalCourses;
        copy.assignedCourses = this.assignedCourses;
        copy.constraintViolations = this.constraintViolations;

        return copy;
    }

    /**
     * Adds an exam assignment to the schedule.
     * Updates the quick lookup structures.
     */
    public void addAssignment(ExamAssignment assignment) {
        String courseCode = assignment.getCourseCode();
        assignments.put(courseCode, assignment);
        totalCourses++;

        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, true);
            assignedCourses++;
        }
    }

    /**
     * Updates an existing assignment with new day, time slot index, and classroom.
     * This is the preferred method for the new day/slot-based system.
     */
    public boolean updateAssignment(String courseCode, int day, int timeSlotIndex, String classroomId) {
        ExamAssignment assignment = assignments.get(courseCode);
        if (assignment == null) {
            return false;
        }

        if (assignment.isLocked()) {
            return false; // Cannot modify locked assignments
        }

        // Remove old assignment from lookup structures
        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, false);
            assignedCourses--;
        }

        // Update assignment
        assignment.setDay(day);
        assignment.setTimeSlotIndex(timeSlotIndex);
        assignment.setClassroomId(classroomId);

        // Also set TimeSlot if configuration is available
        if (configuration != null) {
            assignment.setTimeSlot(configuration.getTimeSlot(day, timeSlotIndex));
        }

        // Add new assignment to lookup structures
        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, true);
            assignedCourses++;
        }

        return true;
    }

    /**
     * Updates an existing assignment with new time slot and classroom.
     * Legacy method for TimeSlot-based system.
     */
    public boolean updateAssignment(String courseCode, TimeSlot timeSlot, String classroomId) {
        ExamAssignment assignment = assignments.get(courseCode);
        if (assignment == null) {
            return false;
        }

        if (assignment.isLocked()) {
            return false; // Cannot modify locked assignments
        }

        // Remove old assignment from lookup structures
        if (assignment.isAssigned() || assignment.isAssignedWithTimeSlot()) {
            updateLookupStructures(assignment, false);
            assignedCourses--;
        }

        // Update assignment
        assignment.setTimeSlot(timeSlot);
        assignment.setClassroomId(classroomId);

        // Add new assignment to lookup structures
        if (assignment.isAssignedWithTimeSlot()) {
            updateLookupStructuresTimeSlot(assignment, true);
            assignedCourses++;
        }

        return true;
    }

    /**
     * Removes an assignment (resets it to unassigned state).
     */
    public boolean removeAssignment(String courseCode) {
        ExamAssignment assignment = assignments.get(courseCode);
        if (assignment == null || assignment.isLocked()) {
            return false;
        }

        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, false);
            assignedCourses--;
        }

        assignment.setDay(-1);
        assignment.setTimeSlotIndex(-1);
        assignment.setTimeSlot(null);
        assignment.setClassroomId(null);

        return true;
    }

    /**
     * Updates the quick lookup structures when an assignment changes.
     * Uses day/timeSlotIndex-based key.
     */
    private void updateLookupStructures(ExamAssignment assignment, boolean add) {
        if (!assignment.isAssigned())
            return;

        String classroomId = assignment.getClassroomId();
        String timeSlotKey = assignment.getTimeKey(); // D0_S0 format
        String courseCode = assignment.getCourseCode();

        if (add) {
            classroomTimeSlotUsage.computeIfAbsent(classroomId, k -> new HashSet<>()).add(timeSlotKey);
            timeSlotCourseMapping.computeIfAbsent(timeSlotKey, k -> new HashSet<>()).add(courseCode);
        } else {
            Set<String> slots = classroomTimeSlotUsage.get(classroomId);
            if (slots != null) {
                slots.remove(timeSlotKey);
            }

            Set<String> courses = timeSlotCourseMapping.get(timeSlotKey);
            if (courses != null) {
                courses.remove(courseCode);
            }
        }
    }

    /**
     * Updates lookup structures using TimeSlot ID (legacy method).
     */
    private void updateLookupStructuresTimeSlot(ExamAssignment assignment, boolean add) {
        if (!assignment.isAssignedWithTimeSlot())
            return;

        String classroomId = assignment.getClassroomId();
        String timeSlotId = assignment.getTimeSlot().getId();
        String courseCode = assignment.getCourseCode();

        if (add) {
            classroomTimeSlotUsage.computeIfAbsent(classroomId, k -> new HashSet<>()).add(timeSlotId);
            timeSlotCourseMapping.computeIfAbsent(timeSlotId, k -> new HashSet<>()).add(courseCode);
        } else {
            Set<String> slots = classroomTimeSlotUsage.get(classroomId);
            if (slots != null) {
                slots.remove(timeSlotId);
            }

            Set<String> courses = timeSlotCourseMapping.get(timeSlotId);
            if (courses != null) {
                courses.remove(courseCode);
            }
        }
    }

    /**
     * Checks if a classroom is available at a given day and time slot.
     */
    public boolean isClassroomAvailable(String classroomId, int day, int timeSlotIndex) {
        String timeSlotKey = "D" + day + "_S" + timeSlotIndex;
        Set<String> usedSlots = classroomTimeSlotUsage.get(classroomId);

        return usedSlots == null || !usedSlots.contains(timeSlotKey);
    }

    /**
     * Checks if a classroom is available at a given time slot (legacy method).
     */
    public boolean isClassroomAvailable(String classroomId, TimeSlot timeSlot) {
        // Check for overlapping time slots
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssignedWithTimeSlot() &&
                    assignment.getClassroomId().equals(classroomId) &&
                    assignment.getTimeSlot().overlapsWith(timeSlot)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets all courses scheduled at a specific day and time slot.
     */
    public Set<String> getCoursesAtDaySlot(int day, int timeSlotIndex) {
        String timeSlotKey = "D" + day + "_S" + timeSlotIndex;
        Set<String> courses = timeSlotCourseMapping.get(timeSlotKey);
        return courses != null ? new HashSet<>(courses) : new HashSet<>();
    }

    /**
     * Gets all courses scheduled at a specific time slot (legacy method).
     */
    public Set<String> getCoursesAtTimeSlot(TimeSlot timeSlot) {
        Set<String> courses = new HashSet<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssignedWithTimeSlot() &&
                    assignment.getTimeSlot().overlapsWith(timeSlot)) {
                courses.add(assignment.getCourseCode());
            }
        }
        return courses;
    }

    /**
     * Gets all unassigned courses.
     */
    public List<ExamAssignment> getUnassignedCourses() {
        List<ExamAssignment> unassigned = new ArrayList<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (!assignment.isAssigned()) {
                unassigned.add(assignment);
            }
        }
        return unassigned;
    }

    /**
     * Gets all assigned courses.
     */
    public List<ExamAssignment> getAssignedCoursesList() {
        List<ExamAssignment> assigned = new ArrayList<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssigned()) {
                assigned.add(assignment);
            }
        }
        return assigned;
    }

    /**
     * Checks if the schedule is complete (all courses are assigned).
     */
    public boolean isComplete() {
        return assignedCourses == totalCourses;
    }

    /**
     * Gets the completion percentage.
     */
    public double getCompletionPercentage() {
        if (totalCourses == 0)
            return 100.0;
        return (assignedCourses * 100.0) / totalCourses;
    }

    /**
     * Gets the schedule as a 2D structure [day][slot] -> list of assignments.
     */
    public Map<String, List<ExamAssignment>> getScheduleByDaySlot() {
        Map<String, List<ExamAssignment>> schedule = new TreeMap<>();

        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssigned()) {
                String key = assignment.getTimeKey();
                schedule.computeIfAbsent(key, k -> new ArrayList<>()).add(assignment);
            }
        }

        return schedule;
    }

    // Getters and Setters

    public Map<String, ExamAssignment> getAssignments() {
        return Collections.unmodifiableMap(assignments);
    }

    public ExamAssignment getAssignment(String courseCode) {
        return assignments.get(courseCode);
    }

    public List<TimeSlot> getAvailableTimeSlots() {
        return availableTimeSlots;
    }

    public void setAvailableTimeSlots(List<TimeSlot> availableTimeSlots) {
        this.availableTimeSlots = availableTimeSlots;
    }

    public List<Classroom> getAvailableClassrooms() {
        return availableClassrooms;
    }

    public void setAvailableClassrooms(List<Classroom> availableClassrooms) {
        this.availableClassrooms = availableClassrooms;
    }

    public ScheduleConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ScheduleConfiguration configuration) {
        this.configuration = configuration;
    }

    public int getTotalCourses() {
        return totalCourses;
    }

    public int getAssignedCourses() {
        return assignedCourses;
    }

    public int getConstraintViolations() {
        return constraintViolations;
    }

    public void setConstraintViolations(int constraintViolations) {
        this.constraintViolations = constraintViolations;
    }

    @Override
    public String toString() {
        return String.format("ScheduleState[%d/%d assigned, %.1f%% complete, %d violations]",
                assignedCourses, totalCourses, getCompletionPercentage(), constraintViolations);
    }

    /**
     * Returns a detailed summary of the schedule.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Schedule State ===\n");
        sb.append(String.format("Assigned: %d/%d (%.1f%%)\n",
                assignedCourses, totalCourses, getCompletionPercentage()));
        sb.append(String.format("Violations: %d\n", constraintViolations));
        sb.append("\nAssignments:\n");

        for (ExamAssignment assignment : assignments.values()) {
            sb.append("  ").append(assignment.toDisplayString()).append("\n");
        }

        return sb.toString();
    }
}
