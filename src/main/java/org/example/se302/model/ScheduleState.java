package org.example.se302.model;

import java.util.*;

/**
 * Represents the complete state of an exam schedule.
 * This is the main data structure used by the CSP solver to track
 * the current schedule state and validate constraints.
 */
public class ScheduleState {
    // All exam assignments indexed by course code
    private Map<String, ExamAssignment> assignments;

    // Quick lookup structures for constraint checking
    private Map<String, Set<String>> classroomTimeSlotUsage; // classroomId -> set of timeSlot IDs
    private Map<String, Set<String>> timeSlotCourseMapping; // timeSlotId -> set of course codes

    // Available resources
    private List<TimeSlot> availableTimeSlots;
    private List<Classroom> availableClassrooms;

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
        this.totalCourses = 0;
        this.assignedCourses = 0;
        this.constraintViolations = 0;
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
     * Updates an existing assignment with new time slot and classroom.
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
        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, false);
            assignedCourses--;
        }

        // Update assignment
        assignment.setTimeSlot(timeSlot);
        assignment.setClassroomId(classroomId);

        // Add new assignment to lookup structures
        if (assignment.isAssigned()) {
            updateLookupStructures(assignment, true);
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

        assignment.setTimeSlot(null);
        assignment.setClassroomId(null);

        return true;
    }

    /**
     * Updates the quick lookup structures when an assignment changes.
     */
    private void updateLookupStructures(ExamAssignment assignment, boolean add) {
        if (!assignment.isAssigned())
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
     * Checks if a classroom is available at a given time slot.
     */
    public boolean isClassroomAvailable(String classroomId, TimeSlot timeSlot) {
        Set<String> usedSlots = classroomTimeSlotUsage.get(classroomId);
        if (usedSlots == null) {
            return true;
        }

        // Check for overlapping time slots
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssigned() &&
                    assignment.getClassroomId().equals(classroomId) &&
                    assignment.getTimeSlot().overlapsWith(timeSlot)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets all courses scheduled at a specific time slot.
     */
    public Set<String> getCoursesAtTimeSlot(TimeSlot timeSlot) {
        Set<String> courses = new HashSet<>();
        for (ExamAssignment assignment : assignments.values()) {
            if (assignment.isAssigned() &&
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
}
