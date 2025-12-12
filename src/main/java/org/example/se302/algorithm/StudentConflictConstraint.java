package org.example.se302.algorithm;

import org.example.se302.model.Course;
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleState;

import java.util.*;

/**
 * Hard constraint: A student cannot have two exams at the same time.
 * This checks for overlapping time slots among courses that share students.
 */
public class StudentConflictConstraint implements Constraint {

    private Map<String, Course> courses;

    public StudentConflictConstraint(Map<String, Course> courses) {
        this.courses = courses;
    }

    @Override
    public String getName() {
        return "STUDENT_CONFLICT";
    }

    @Override
    public String getDescription() {
        return "A student cannot have two exams at the same time";
    }

    @Override
    public boolean isSatisfied(ExamAssignment assignment, ScheduleState state) {
        if (!assignment.isAssigned()) {
            return true;
        }

        Course course = courses.get(assignment.getCourseCode());
        if (course == null) {
            return true; // Unknown course, skip check
        }

        Set<String> enrolledStudents = new HashSet<>(course.getEnrolledStudents());

        // Get all courses scheduled at overlapping time slots
        Set<String> coursesAtSameTime = state.getCoursesAtTimeSlot(assignment.getTimeSlot());

        for (String otherCourseCode : coursesAtSameTime) {
            if (otherCourseCode.equals(assignment.getCourseCode())) {
                continue; // Skip self
            }

            Course otherCourse = courses.get(otherCourseCode);
            if (otherCourse == null) {
                continue;
            }

            // Check if any student is enrolled in both courses
            for (String student : otherCourse.getEnrolledStudents()) {
                if (enrolledStudents.contains(student)) {
                    return false; // Conflict found
                }
            }
        }

        return true;
    }

    @Override
    public int getPriority() {
        return 100; // High priority - hard constraint
    }

    @Override
    public boolean isHard() {
        return true;
    }

    @Override
    public String getViolationMessage(ExamAssignment assignment, ScheduleState state) {
        if (isSatisfied(assignment, state)) {
            return null;
        }

        Course course = courses.get(assignment.getCourseCode());
        if (course == null) {
            return null;
        }

        Set<String> enrolledStudents = new HashSet<>(course.getEnrolledStudents());
        Set<String> coursesAtSameTime = state.getCoursesAtTimeSlot(assignment.getTimeSlot());

        for (String otherCourseCode : coursesAtSameTime) {
            if (otherCourseCode.equals(assignment.getCourseCode())) {
                continue;
            }

            Course otherCourse = courses.get(otherCourseCode);
            if (otherCourse == null) {
                continue;
            }

            List<String> conflictingStudents = new ArrayList<>();
            for (String student : otherCourse.getEnrolledStudents()) {
                if (enrolledStudents.contains(student)) {
                    conflictingStudents.add(student);
                }
            }

            if (!conflictingStudents.isEmpty()) {
                return String.format("Students %s have exams for both %s and %s at %s",
                        conflictingStudents.size() > 3 ? conflictingStudents.subList(0, 3) + "..."
                                : conflictingStudents,
                        assignment.getCourseCode(), otherCourseCode, assignment.getTimeSlot());
            }
        }

        return null;
    }

    public void setCourses(Map<String, Course> courses) {
        this.courses = courses;
    }

    /**
     * Gets the set of courses that conflict with the given course
     * (i.e., share at least one student).
     */
    public Set<String> getConflictingCourses(String courseCode) {
        Set<String> conflicting = new HashSet<>();
        Course course = courses.get(courseCode);
        if (course == null) {
            return conflicting;
        }

        Set<String> enrolledStudents = new HashSet<>(course.getEnrolledStudents());

        for (Map.Entry<String, Course> entry : courses.entrySet()) {
            if (entry.getKey().equals(courseCode)) {
                continue;
            }

            for (String student : entry.getValue().getEnrolledStudents()) {
                if (enrolledStudents.contains(student)) {
                    conflicting.add(entry.getKey());
                    break;
                }
            }
        }

        return conflicting;
    }
}
