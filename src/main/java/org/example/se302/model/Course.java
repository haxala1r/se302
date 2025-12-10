package org.example.se302.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a course in the exam scheduling system.
 */
public class Course {
    private String courseCode;
    private List<String> enrolledStudents;
    private String assignedClassroom;  // null if not scheduled
    private String examDateTime;       // null if not scheduled

    public Course(String courseCode) {
        this.courseCode = courseCode;
        this.enrolledStudents = new ArrayList<>();
        this.assignedClassroom = null;
        this.examDateTime = null;
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

    public void setEnrolledStudents(List<String> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public void addStudent(String studentId) {
        if (!enrolledStudents.contains(studentId)) {
            enrolledStudents.add(studentId);
        }
    }

    public int getEnrolledStudentsCount() {
        return enrolledStudents.size();
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
}
