package org.example.se302.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a course in the exam scheduling system.
 * Contains course information and exam scheduling details.
 */
public class Course {
    private String courseCode;
    private List<String> enrolledStudents;

    // Exam schedule fields (index-based)
    private int examDay; // -1 if not scheduled, 0-based day index
    private int examTimeSlot; // -1 if not scheduled, 0-based slot index
    private String assignedClassroom; // null if not scheduled

    // Legacy field for backward compatibility
    private String examDateTime; // null if not scheduled (string format)

    public Course(String courseCode) {
        this.courseCode = courseCode;
        this.enrolledStudents = new ArrayList<>();
        this.examDay = -1;
        this.examTimeSlot = -1;
        this.assignedClassroom = null;
        this.examDateTime = null;
    }

    /**
     * Checks if this course has been scheduled for an exam.
     */
    public boolean isScheduled() {
        return examDay >= 0 && examTimeSlot >= 0 && assignedClassroom != null;
    }

    /**
     * Clears the exam schedule for this course.
     */
    public void clearSchedule() {
        this.examDay = -1;
        this.examTimeSlot = -1;
        this.assignedClassroom = null;
        this.examDateTime = null;
    }

    /**
     * Sets the complete exam schedule.
     * 
     * @param day         Day index (0-based)
     * @param timeSlot    Time slot index (0-based)
     * @param classroomId Classroom ID
     */
    public void setExamSchedule(int day, int timeSlot, String classroomId) {
        this.examDay = day;
        this.examTimeSlot = timeSlot;
        this.assignedClassroom = classroomId;
    }

    /**
     * Gets a unique key for this course's time slot.
     */
    public String getTimeSlotKey() {
        if (!isScheduled()) {
            return null;
        }
        return "D" + examDay + "_S" + examTimeSlot;
    }

    // Basic getters and setters

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public List<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(List<String> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public void addStudent(String studentId) {
        if (!enrolledStudents.contains(studentId)) {
            enrolledStudents.add(studentId);
        }
    }

    public void removeStudent(String studentId) {
        enrolledStudents.remove(studentId);
    }

    public int getEnrolledStudentsCount() {
        return enrolledStudents.size();
    }

    // Schedule field getters and setters

    public int getExamDay() {
        return examDay;
    }

    public void setExamDay(int examDay) {
        this.examDay = examDay;
    }

    public int getExamTimeSlot() {
        return examTimeSlot;
    }

    public void setExamTimeSlot(int examTimeSlot) {
        this.examTimeSlot = examTimeSlot;
    }

    public String getAssignedClassroom() {
        return assignedClassroom;
    }

    public void setAssignedClassroom(String assignedClassroom) {
        this.assignedClassroom = assignedClassroom;
    }

    public String getExamDateTime() {
        return examDateTime;
    }

    public void setExamDateTime(String examDateTime) {
        this.examDateTime = examDateTime;
    }

    @Override
    public String toString() {
        return courseCode;
    }

    /**
     * Returns a detailed string representation including schedule info.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(courseCode);
        sb.append(" (").append(enrolledStudents.size()).append(" students)");

        if (isScheduled()) {
            sb.append(" - Day ").append(examDay + 1);
            sb.append(", Slot ").append(examTimeSlot + 1);
            sb.append(", Room: ").append(assignedClassroom);
        } else {
            sb.append(" - Not Scheduled");
        }

        return sb.toString();
    }
}
