package org.example.se302.model;

import java.util.Objects;

/**
 * Represents an exam assignment in the CSP-based scheduling system.
 * An ExamAssignment links a course to a specific time slot and classroom.
 * This is the core data structure for the schedule state.
 */
public class ExamAssignment {
    private String courseCode;
    private TimeSlot timeSlot;
    private String classroomId;
    private int studentCount;

    // Assignment status
    private boolean isLocked; // If true, this assignment cannot be changed by the algorithm

    /**
     * Creates a new exam assignment.
     * 
     * @param courseCode  The course code for this exam
     * @param timeSlot    The time slot assigned to this exam
     * @param classroomId The classroom assigned to this exam
     */
    public ExamAssignment(String courseCode, TimeSlot timeSlot, String classroomId) {
        this.courseCode = courseCode;
        this.timeSlot = timeSlot;
        this.classroomId = classroomId;
        this.studentCount = 0;
        this.isLocked = false;
    }

    /**
     * Creates an unassigned exam (only course is known).
     */
    public ExamAssignment(String courseCode) {
        this(courseCode, null, null);
    }

    /**
     * Creates a deep copy of this assignment.
     */
    public ExamAssignment copy() {
        ExamAssignment copy = new ExamAssignment(this.courseCode, this.timeSlot, this.classroomId);
        copy.studentCount = this.studentCount;
        copy.isLocked = this.isLocked;
        return copy;
    }

    /**
     * Checks if this assignment is complete (has both time slot and classroom).
     */
    public boolean isAssigned() {
        return timeSlot != null && classroomId != null;
    }

    /**
     * Checks if this assignment conflicts with another assignment.
     * Two assignments conflict if:
     * 1. Same classroom at overlapping time slots
     * 2. (Checked separately) Same student in both courses at overlapping time
     * slots
     */
    public boolean conflictsWith(ExamAssignment other) {
        if (!this.isAssigned() || !other.isAssigned()) {
            return false;
        }

        // Check classroom conflict
        if (this.classroomId.equals(other.classroomId) &&
                this.timeSlot.overlapsWith(other.timeSlot)) {
            return true;
        }

        return false;
    }

    // Getters and Setters

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExamAssignment that = (ExamAssignment) o;
        return Objects.equals(courseCode, that.courseCode) &&
                Objects.equals(timeSlot, that.timeSlot) &&
                Objects.equals(classroomId, that.classroomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode, timeSlot, classroomId);
    }

    @Override
    public String toString() {
        if (!isAssigned()) {
            return courseCode + " [Unassigned]";
        }
        return courseCode + " -> " + classroomId + " @ " + timeSlot;
    }
}
