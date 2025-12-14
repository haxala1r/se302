package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;

/**
 * Validates scheduling constraints for exam assignments.
 * Ensures that all hard constraints are satisfied.
 * Works with the day/timeSlotIndex based ExamAssignment architecture.
 */
public class ConstraintValidator {
    private final DataManager dataManager;

    public ConstraintValidator() {
        this.dataManager = DataManager.getInstance();
    }

    /**
     * Validate all constraints for a complete schedule.
     */
    public ValidationResult validateSchedule(ScheduleState scheduleState) {
        ValidationResult result = new ValidationResult();

        // Check all assignments
        for (ExamAssignment assignment : scheduleState.getAssignments().values()) {
            if (assignment.isAssigned()) {
                ValidationResult assignmentResult = validateAssignment(assignment, scheduleState);
                result.merge(assignmentResult);
            }
        }

        return result;
    }

    /**
     * Validate a single assignment against the current schedule state.
     * Uses default strict validation (no back-to-back allowed).
     */
    public ValidationResult validateAssignment(ExamAssignment assignment, ScheduleState scheduleState) {
        return validateAssignment(assignment, scheduleState, false);
    }

    /**
     * Validate a single assignment against the current schedule state.
     * 
     * @param allowBackToBack If true, consecutive exams are allowed
     */
    public ValidationResult validateAssignment(ExamAssignment assignment, ScheduleState scheduleState,
            boolean allowBackToBack) {
        ValidationResult result = new ValidationResult();

        if (!assignment.isAssigned()) {
            result.addError("Assignment is not complete (missing time slot or classroom)");
            return result;
        }

        // Check classroom capacity
        ValidationResult capacityResult = checkClassroomCapacity(assignment);
        result.merge(capacityResult);

        // Check no double-booking (always required)
        ValidationResult doubleBookingResult = checkNoDoubleBooking(assignment, scheduleState);
        result.merge(doubleBookingResult);

        // Check student constraints - only check same time slot conflicts (always
        // required)
        Course course = dataManager.getCourse(assignment.getCourseCode());
        if (course != null) {
            // Check if student has another exam at the SAME time slot (hard constraint)
            ValidationResult sameTimeResult = checkNoSameTimeExams(assignment, scheduleState, course);
            result.merge(sameTimeResult);

            // Only check consecutive and max per day if back-to-back is NOT allowed
            if (!allowBackToBack) {
                for (String studentId : course.getEnrolledStudents()) {
                    // Check no consecutive exams (soft - skip if allowBackToBack)
                    ValidationResult consecutiveResult = checkNoConsecutiveExams(
                            studentId, assignment, scheduleState);
                    result.merge(consecutiveResult);

                    // Check max 2 exams per day
                    ValidationResult maxPerDayResult = checkMaxTwoExamsPerDay(
                            studentId, assignment, scheduleState);
                    result.merge(maxPerDayResult);

                    // If already invalid, no need to check more students
                    if (!result.isValid()) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check that no student has two exams at the exact same time slot.
     * This is a hard constraint that must always be satisfied.
     */
    private ValidationResult checkNoSameTimeExams(ExamAssignment newAssignment,
            ScheduleState scheduleState,
            Course course) {
        ValidationResult result = new ValidationResult();

        int newDay = newAssignment.getDay();
        int newSlot = newAssignment.getTimeSlotIndex();

        for (ExamAssignment existing : scheduleState.getAssignments().values()) {
            if (!existing.isAssigned() || existing.getCourseCode().equals(newAssignment.getCourseCode())) {
                continue;
            }

            // Check if same day and slot
            if (existing.getDay() == newDay && existing.getTimeSlotIndex() == newSlot) {
                // Check if any student is enrolled in both courses
                Course otherCourse = dataManager.getCourse(existing.getCourseCode());
                if (otherCourse != null) {
                    for (String studentId : course.getEnrolledStudents()) {
                        if (otherCourse.getEnrolledStudents().contains(studentId)) {
                            result.addError(String.format(
                                    "Student %s has two exams at the same time: %s and %s (Day %d, Slot %d)",
                                    studentId, newAssignment.getCourseCode(), existing.getCourseCode(),
                                    newDay + 1, newSlot + 1));
                            return result; // One conflict is enough
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check that classroom capacity is not exceeded.
     */
    public ValidationResult checkClassroomCapacity(ExamAssignment assignment) {
        ValidationResult result = new ValidationResult();

        Classroom classroom = dataManager.getClassroom(assignment.getClassroomId());
        Course course = dataManager.getCourse(assignment.getCourseCode());

        if (classroom == null) {
            result.addError("Classroom not found: " + assignment.getClassroomId());
            return result;
        }

        if (course == null) {
            result.addError("Course not found: " + assignment.getCourseCode());
            return result;
        }

        int enrolledCount = course.getEnrolledStudentsCount();
        int capacity = classroom.getCapacity();

        if (enrolledCount > capacity) {
            result.addError(String.format(
                    "Classroom capacity exceeded: %s has %d students but %s capacity is %d",
                    course.getCourseCode(), enrolledCount, classroom.getClassroomId(), capacity));
        }

        return result;
    }

    /**
     * Check that no classroom is double-booked at the same time.
     */
    public ValidationResult checkNoDoubleBooking(ExamAssignment assignment, ScheduleState scheduleState) {
        ValidationResult result = new ValidationResult();

        // Check if any other assignment uses the same classroom at the same time
        for (ExamAssignment existing : scheduleState.getAssignments().values()) {
            if (existing.isAssigned() &&
                    !existing.getCourseCode().equals(assignment.getCourseCode()) &&
                    existing.getClassroomId().equals(assignment.getClassroomId()) &&
                    existing.getDay() == assignment.getDay() &&
                    existing.getTimeSlotIndex() == assignment.getTimeSlotIndex()) {

                result.addError(String.format(
                        "Classroom double-booking: %s already has %s at Day %d, Slot %d",
                        assignment.getClassroomId(),
                        existing.getCourseCode(),
                        assignment.getDay() + 1,
                        assignment.getTimeSlotIndex() + 1));
                break;
            }
        }

        return result;
    }

    /**
     * Check that a student has no consecutive exams.
     * Consecutive means exams in adjacent time slots on the same day
     * (back-to-back).
     */
    public ValidationResult checkNoConsecutiveExams(String studentId,
            ExamAssignment newAssignment,
            ScheduleState scheduleState) {
        ValidationResult result = new ValidationResult();

        // Get all courses this student is enrolled in
        Student student = dataManager.getStudent(studentId);
        if (student == null) {
            return result; // Student not found, skip
        }

        List<String> studentCourses = student.getEnrolledCourses();
        int newDay = newAssignment.getDay();
        int newSlot = newAssignment.getTimeSlotIndex();

        // Check each existing assignment for this student
        for (ExamAssignment existing : scheduleState.getAssignments().values()) {
            if (!existing.isAssigned() || existing.getCourseCode().equals(newAssignment.getCourseCode())) {
                continue;
            }

            // Check if this assignment is for a course the student is enrolled in
            if (studentCourses.contains(existing.getCourseCode())) {
                int existingDay = existing.getDay();
                int existingSlot = existing.getTimeSlotIndex();

                // Check if consecutive (adjacent slots on the same day)
                boolean isConsecutive = (existingDay == newDay) &&
                        (Math.abs(existingSlot - newSlot) == 1);

                if (isConsecutive) {
                    result.addError(String.format(
                            "Consecutive exams for student %s: %s (Day %d, Slot %d) and %s (Day %d, Slot %d) are back-to-back",
                            studentId,
                            existing.getCourseCode(), existingDay + 1, existingSlot + 1,
                            newAssignment.getCourseCode(), newDay + 1, newSlot + 1));
                }
            }
        }

        return result;
    }

    /**
     * Check that a student has at most 2 exams per day.
     */
    public ValidationResult checkMaxTwoExamsPerDay(String studentId,
            ExamAssignment newAssignment,
            ScheduleState scheduleState) {
        ValidationResult result = new ValidationResult();

        // Get all courses this student is enrolled in
        Student student = dataManager.getStudent(studentId);
        if (student == null) {
            return result;
        }

        List<String> studentCourses = student.getEnrolledCourses();
        int newDay = newAssignment.getDay();

        // Count exams on the same day as new assignment
        long examsOnSameDay = scheduleState.getAssignments().values().stream()
                .filter(a -> a.isAssigned())
                .filter(a -> !a.getCourseCode().equals(newAssignment.getCourseCode()))
                .filter(a -> studentCourses.contains(a.getCourseCode()))
                .filter(a -> a.getDay() == newDay)
                .count();

        // Including the new assignment, would be examsOnSameDay + 1
        if (examsOnSameDay + 1 > 2) {
            result.addError(String.format(
                    "Too many exams for student %s on Day %d: would have %d exams (max 2)",
                    studentId,
                    newDay + 1,
                    examsOnSameDay + 1));
        }

        return result;
    }

    /**
     * Get all students affected by an assignment.
     */
    public List<String> getAffectedStudents(ExamAssignment assignment) {
        Course course = dataManager.getCourse(assignment.getCourseCode());
        return course != null ? course.getEnrolledStudents() : new ArrayList<>();
    }

    /**
     * Check if a specific assignment would create conflicts.
     * Returns list of conflict descriptions.
     */
    public List<String> getConflictsForAssignment(ExamAssignment assignment, ScheduleState scheduleState) {
        ValidationResult result = validateAssignment(assignment, scheduleState);
        return result.getErrors();
    }

    /**
     * Result of constraint validation.
     */
    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        public void addError(String error) {
            if (!errors.contains(error)) { // Avoid duplicates
                errors.add(error);
            }
        }

        public void addWarning(String warning) {
            if (!warnings.contains(warning)) {
                warnings.add(warning);
            }
        }

        public void merge(ValidationResult other) {
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public int getErrorCount() {
            return errors.size();
        }

        public String getFormattedErrors() {
            if (errors.isEmpty()) {
                return "No errors";
            }
            return String.join("\n", errors);
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{errors=%d, warnings=%d}",
                    errors.size(), warnings.size());
        }
    }
}
