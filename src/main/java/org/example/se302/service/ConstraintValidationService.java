package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;

/**
 * Service for validating exam assignment changes against constraints.
 * Provides detailed violation information including affected student names.
 */
public class ConstraintValidationService {

    private final DataManager dataManager;

    public ConstraintValidationService() {
        this.dataManager = DataManager.getInstance();
    }

    /**
     * Validates a proposed exam assignment change.
     *
     * @param courseCode   The course being moved
     * @param newDay       Proposed day (0-based)
     * @param newSlot      Proposed time slot (0-based)
     * @param newClassroom Proposed classroom ID
     * @param currentState Current schedule state (to check against other exams)
     * @return ValidationResult with all violations found
     */
    public ValidationResult validateAssignment(String courseCode, int newDay, int newSlot,
            String newClassroom, ScheduleState currentState) {
        ValidationResult result = new ValidationResult();

        // Get course info
        Course course = dataManager.getCourse(courseCode);
        if (course == null) {
            result.addViolation(new ConstraintViolation(
                    "Unknown Course",
                    true,
                    "Course " + courseCode + " not found",
                    Collections.emptyList(),
                    null));
            return result;
        }

        // Check classroom capacity
        Classroom classroom = dataManager.getClassroom(newClassroom);
        if (classroom == null) {
            result.addViolation(new ConstraintViolation(
                    "Unknown Classroom",
                    true,
                    "Classroom " + newClassroom + " not found",
                    Collections.emptyList(),
                    null));
        } else if (classroom.getCapacity() < course.getEnrolledStudentsCount()) {
            result.addViolation(new ConstraintViolation(
                    "Capacity Exceeded",
                    true,
                    String.format("Classroom %s has capacity %d, but course has %d students",
                            newClassroom, classroom.getCapacity(), course.getEnrolledStudentsCount()),
                    Collections.emptyList(),
                    null));
        }

        // Check classroom conflict (another exam in same classroom at same time)
        String classroomConflictCourse = getClassroomConflict(courseCode, newClassroom, newDay, newSlot, currentState);
        if (classroomConflictCourse != null) {
            result.addViolation(new ConstraintViolation(
                    "Classroom Conflict",
                    true,
                    String.format("Classroom %s is already used by %s at this time",
                            newClassroom, classroomConflictCourse),
                    Collections.emptyList(),
                    classroomConflictCourse));
        }

        // Check student conflicts (students with exams at the same time)
        List<StudentConflictInfo> studentConflicts = getStudentConflicts(courseCode, newDay, newSlot, currentState);
        if (!studentConflicts.isEmpty()) {
            // Group by conflicting course
            Map<String, List<String>> conflictsByCourse = new LinkedHashMap<>();
            for (StudentConflictInfo conflict : studentConflicts) {
                conflictsByCourse
                        .computeIfAbsent(conflict.conflictingCourse, k -> new ArrayList<>())
                        .add(conflict.studentId);
            }

            for (Map.Entry<String, List<String>> entry : conflictsByCourse.entrySet()) {
                String conflictCourse = entry.getKey();
                List<String> students = entry.getValue();

                String studentList = formatStudentList(students);

                result.addViolation(new ConstraintViolation(
                        "Student Conflict",
                        true,
                        String.format("%d student(s) have exams for both %s and %s at this time: %s",
                                students.size(), courseCode, conflictCourse, studentList),
                        new ArrayList<>(students),
                        conflictCourse));
            }
        }

        return result;
    }

    /**
     * Gets the course that conflicts with the given classroom at the specified
     * time.
     */
    private String getClassroomConflict(String excludeCourse, String classroomId,
            int day, int slot, ScheduleState state) {
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.getCourseCode().equals(excludeCourse)) {
                continue; // Skip the course being moved
            }
            if (assignment.isAssigned() &&
                    assignment.getDay() == day &&
                    assignment.getTimeSlotIndex() == slot &&
                    classroomId.equals(assignment.getClassroomId())) {
                return assignment.getCourseCode();
            }
        }
        return null;
    }

    /**
     * Gets all student conflicts for a proposed assignment.
     */
    private List<StudentConflictInfo> getStudentConflicts(String courseCode, int day, int slot,
            ScheduleState state) {
        List<StudentConflictInfo> conflicts = new ArrayList<>();

        Course course = dataManager.getCourse(courseCode);
        if (course == null) {
            return conflicts;
        }

        Set<String> enrolledStudents = new HashSet<>(course.getEnrolledStudents());

        // Find all other courses at the same time
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.getCourseCode().equals(courseCode)) {
                continue; // Skip self
            }
            if (!assignment.isAssigned() ||
                    assignment.getDay() != day ||
                    assignment.getTimeSlotIndex() != slot) {
                continue; // Not at the same time
            }

            Course otherCourse = dataManager.getCourse(assignment.getCourseCode());
            if (otherCourse == null) {
                continue;
            }

            // Check for shared students
            for (String studentId : otherCourse.getEnrolledStudents()) {
                if (enrolledStudents.contains(studentId)) {
                    conflicts.add(new StudentConflictInfo(studentId, assignment.getCourseCode()));
                }
            }
        }

        return conflicts;
    }

    /**
     * Formats a list of student IDs for display.
     */
    private String formatStudentList(List<String> students) {
        if (students.size() <= 5) {
            return String.join(", ", students);
        } else {
            List<String> first5 = students.subList(0, 5);
            return String.join(", ", first5) + " and " + (students.size() - 5) + " more";
        }
    }

    // ============= Inner Classes =============

    /**
     * Holds information about a student conflict.
     */
    private static class StudentConflictInfo {
        final String studentId;
        final String conflictingCourse;

        StudentConflictInfo(String studentId, String conflictingCourse) {
            this.studentId = studentId;
            this.conflictingCourse = conflictingCourse;
        }
    }

    /**
     * Represents the result of a constraint validation.
     */
    public static class ValidationResult {
        private final List<ConstraintViolation> violations = new ArrayList<>();

        public void addViolation(ConstraintViolation violation) {
            violations.add(violation);
        }

        public List<ConstraintViolation> getViolations() {
            return Collections.unmodifiableList(violations);
        }

        public boolean isValid() {
            return violations.isEmpty();
        }

        public boolean hasHardViolations() {
            return violations.stream().anyMatch(ConstraintViolation::isHard);
        }

        public boolean hasSoftViolations() {
            return violations.stream().anyMatch(v -> !v.isHard());
        }

        public String getFormattedMessage() {
            if (violations.isEmpty()) {
                return "✓ No constraint violations";
            }

            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation v : violations) {
                sb.append(v.isHard() ? "❌ " : "⚠️ ");
                sb.append(v.getConstraintName()).append(": ");
                sb.append(v.getMessage()).append("\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * Represents a single constraint violation.
     */
    public static class ConstraintViolation {
        private final String constraintName;
        private final boolean isHard;
        private final String message;
        private final List<String> affectedStudents;
        private final String conflictingCourse;

        public ConstraintViolation(String constraintName, boolean isHard, String message,
                List<String> affectedStudents, String conflictingCourse) {
            this.constraintName = constraintName;
            this.isHard = isHard;
            this.message = message;
            this.affectedStudents = affectedStudents != null ? affectedStudents : Collections.emptyList();
            this.conflictingCourse = conflictingCourse;
        }

        public String getConstraintName() {
            return constraintName;
        }

        public boolean isHard() {
            return isHard;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getAffectedStudents() {
            return affectedStudents;
        }

        public String getConflictingCourse() {
            return conflictingCourse;
        }
    }
}
