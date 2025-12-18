package org.example.se302.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a course with exam scheduling details.
 */
public class Course {
    private String courseCode;
    private List<String> enrolledStudents;
    private int examDay = -1;
    private int examTimeSlot = -1;
    private String assignedClassroom;

    public Course(String courseCode) {
        this.courseCode = courseCode;
        this.enrolledStudents = new ArrayList<>();
    }

    public boolean isScheduled() {
        return examDay >= 0 && examTimeSlot >= 0 && assignedClassroom != null;
    }

    public void clearSchedule() {
        this.examDay = -1;
        this.examTimeSlot = -1;
        this.assignedClassroom = null;
    }

    public void setExamSchedule(int day, int timeSlot, String classroomId) {
        this.examDay = day;
        this.examTimeSlot = timeSlot;
        this.assignedClassroom = classroomId;
    }

    public String getTimeSlotKey() {
        return isScheduled() ? "D" + examDay + "_S" + examTimeSlot : null;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public List<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(List<String> students) {
        this.enrolledStudents = students;
    }

    public void addStudent(String studentId) {
        if (!enrolledStudents.contains(studentId))
            enrolledStudents.add(studentId);
    }

    public void removeStudent(String studentId) {
        enrolledStudents.remove(studentId);
    }

    public int getEnrolledStudentsCount() {
        return enrolledStudents.size();
    }

    public int getExamDay() {
        return examDay;
    }

    public void setExamDay(int examDay) {
        this.examDay = examDay;
    }

    public int getExamTimeSlot() {
        return examTimeSlot;
    }

    public void setExamTimeSlot(int slot) {
        this.examTimeSlot = slot;
    }

    public String getAssignedClassroom() {
        return assignedClassroom;
    }

    public void setAssignedClassroom(String classroom) {
        this.assignedClassroom = classroom;
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
