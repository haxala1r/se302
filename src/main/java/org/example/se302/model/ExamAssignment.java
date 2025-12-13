package org.example.se302.model;

import java.util.Objects;

/**
 * Represents an exam assignment in the CSP-based scheduling system.
 * An ExamAssignment links a course to a specific day, time slot, and classroom.
 * This is the core data structure for the schedule state.
 * 
 * <h3>Assignment Structure:</h3>
 * 
 * <pre>
 * ExamAssignment = {
 *     courseCode:    "CourseCode_01"
 *     classroomId:   "Classroom_01"  
 *     day:           0-based index (0 = Day 1, 1 = Day 2, ...)
 *     timeSlotIndex: 0-based index (0 = first slot, 1 = second slot, ...)
 * }
 * </pre>
 */
public class ExamAssignment {
    private String courseCode;
    private String classroomId;
    private int day; // 0-based day index (-1 if unassigned)
    private int timeSlotIndex; // 0-based time slot index (-1 if unassigned)
    private int studentCount;

    // Optional: reference to the TimeSlot object for detailed time info
    private TimeSlot timeSlot;

    // Assignment status
    private boolean isLocked; // If true, this assignment cannot be changed by the algorithm

    /**
     * Creates a new exam assignment with all details.
     * 
     * @param courseCode    The course code for this exam
     * @param classroomId   The classroom assigned to this exam
     * @param day           The day index (0-based)
     * @param timeSlotIndex The time slot index within the day (0-based)
     */
    public ExamAssignment(String courseCode, String classroomId, int day, int timeSlotIndex) {
        this.courseCode = courseCode;
        this.classroomId = classroomId;
        this.day = day;
        this.timeSlotIndex = timeSlotIndex;
        this.studentCount = 0;
        this.isLocked = false;
        this.timeSlot = null;
    }

    /**
     * Creates a new exam assignment with TimeSlot object.
     * 
     * @param courseCode  The course code for this exam
     * @param timeSlot    The time slot assigned to this exam
     * @param classroomId The classroom assigned to this exam
     */
    public ExamAssignment(String courseCode, TimeSlot timeSlot, String classroomId) {
        this.courseCode = courseCode;
        this.timeSlot = timeSlot;
        this.classroomId = classroomId;
        this.day = -1;
        this.timeSlotIndex = -1;
        this.studentCount = 0;
        this.isLocked = false;
    }

    /**
     * Creates an unassigned exam (only course is known).
     */
    public ExamAssignment(String courseCode) {
        this.courseCode = courseCode;
        this.classroomId = null;
        this.day = -1;
        this.timeSlotIndex = -1;
        this.studentCount = 0;
        this.isLocked = false;
        this.timeSlot = null;
    }

    /**
     * Creates a deep copy of this assignment.
     */
    public ExamAssignment copy() {
        ExamAssignment copy = new ExamAssignment(this.courseCode, this.classroomId, this.day, this.timeSlotIndex);
        copy.studentCount = this.studentCount;
        copy.isLocked = this.isLocked;
        copy.timeSlot = this.timeSlot;
        return copy;
    }

    /**
     * Checks if this assignment is complete (has classroom, day and time slot).
     */
    public boolean isAssigned() {
        return classroomId != null && day >= 0 && timeSlotIndex >= 0;
    }

    /**
     * Checks if this assignment is assigned using TimeSlot object.
     */
    public boolean isAssignedWithTimeSlot() {
        return timeSlot != null && classroomId != null;
    }

    /**
     * Checks if this assignment conflicts with another assignment.
     * Two assignments conflict if:
     * 1. Same classroom at the same day and time slot
     * 2. (Checked separately) Same student in both courses at the same day/time
     */
    public boolean conflictsWith(ExamAssignment other) {
        if (!this.isAssigned() || !other.isAssigned()) {
            return false;
        }

        // Check classroom conflict - same room, same day, same slot
        if (this.classroomId.equals(other.classroomId) &&
                this.day == other.day &&
                this.timeSlotIndex == other.timeSlotIndex) {
            return true;
        }

        return false;
    }

    /**
     * Checks if this exam is at the same time as another (regardless of classroom).
     */
    public boolean sameTimeAs(ExamAssignment other) {
        if (!this.isAssigned() || !other.isAssigned()) {
            return false;
        }
        return this.day == other.day && this.timeSlotIndex == other.timeSlotIndex;
    }

    /**
     * Gets a unique key for the (day, timeSlot) combination.
     */
    public String getTimeKey() {
        return "D" + day + "_S" + timeSlotIndex;
    }

    /**
     * Gets a unique key for the (day, timeSlot, classroom) combination.
     */
    public String getFullKey() {
        return "D" + day + "_S" + timeSlotIndex + "_" + classroomId;
    }

    // Getters and Setters

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getTimeSlotIndex() {
        return timeSlotIndex;
    }

    public void setTimeSlotIndex(int timeSlotIndex) {
        this.timeSlotIndex = timeSlotIndex;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
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
        return day == that.day &&
                timeSlotIndex == that.timeSlotIndex &&
                Objects.equals(courseCode, that.courseCode) &&
                Objects.equals(classroomId, that.classroomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode, classroomId, day, timeSlotIndex);
    }

    @Override
    public String toString() {
        if (!isAssigned()) {
            return courseCode + " [Unassigned]";
        }
        return courseCode + " -> " + classroomId + " @ Day" + (day + 1) + " Slot" + (timeSlotIndex + 1);
    }

    /**
     * Returns a human-readable description of the assignment.
     */
    public String toDisplayString() {
        if (!isAssigned()) {
            return courseCode + " - Not Scheduled";
        }
        return String.format("%s | Day %d, Slot %d | Room: %s | Students: %d",
                courseCode, day + 1, timeSlotIndex + 1, classroomId, studentCount);
    }
}
