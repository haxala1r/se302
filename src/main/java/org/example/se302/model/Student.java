package org.example.se302.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student in the exam scheduling system.
 */
public class Student {
    private String studentId;
    private List<String> enrolledCourses;

    public Student(String studentId) {
        this.studentId = studentId;
        this.enrolledCourses = new ArrayList<>();
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public List<String> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<String> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    public void addCourse(String courseCode) {
        if (!enrolledCourses.contains(courseCode)) {
            enrolledCourses.add(courseCode);
        }
    }

    public int getEnrolledCoursesCount() {
        return enrolledCourses.size();
    }

    @Override
    public String toString() {
        return studentId;
    }
}
