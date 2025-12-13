package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for generating exam schedules using Constraint Satisfaction Problem (CSP) solving.
 * Implements backtracking algorithm with MRV and LCV heuristics.
 * Works with the day/timeSlotIndex based ExamAssignment architecture.
 */
public class ScheduleGeneratorService {
    private final DataManager dataManager;
    private final ConstraintValidator validator;
    private final AtomicBoolean cancelled;
    private ProgressListener progressListener;

    public ScheduleGeneratorService() {
        this.dataManager = DataManager.getInstance();
        this.validator = new ConstraintValidator();
        this.cancelled = new AtomicBoolean(false);
    }

    /**
     * Generate an exam schedule based on configuration.
     */
    public ScheduleResult generateSchedule(ScheduleConfiguration config) {
        cancelled.set(false);

        // Validate input
        if (dataManager.getTotalCourses() == 0) {
            return ScheduleResult.failure("No courses to schedule");
        }

        if (dataManager.getTotalClassrooms() == 0) {
            return ScheduleResult.failure("No classrooms available");
        }

        // Create schedule state
        ScheduleState scheduleState = initializeScheduleState(config);

        // Get courses ordered by MRV heuristic
        List<Course> coursesToSchedule = getCoursesOrderedByMRV();

        // Report progress
        updateProgress(0, coursesToSchedule.size(), "Starting schedule generation...");

        // Start backtracking
        boolean success = backtrack(scheduleState, coursesToSchedule, 0, config);

        if (cancelled.get()) {
            return ScheduleResult.cancelled();
        }

        if (success) {
            updateProgress(coursesToSchedule.size(), coursesToSchedule.size(), "Schedule generated successfully!");
            return ScheduleResult.success(scheduleState);
        } else {
            return ScheduleResult.failure("No valid schedule found. Try increasing days/slots or relaxing constraints.");
        }
    }

    /**
     * Initialize ScheduleState with configuration.
     */
    private ScheduleState initializeScheduleState(ScheduleConfiguration config) {
        ScheduleState state = new ScheduleState();

        // Set available classrooms
        state.setAvailableClassrooms(new ArrayList<>(dataManager.getClassrooms()));

        // Initialize exam assignments for all courses (unassigned)
        for (Course course : dataManager.getCourses()) {
            ExamAssignment assignment = new ExamAssignment(course.getCourseCode());
            assignment.setStudentCount(course.getEnrolledStudentsCount());
            state.addAssignment(assignment);
        }

        return state;
    }

    /**
     * Backtracking algorithm core.
     */
    private boolean backtrack(ScheduleState scheduleState, List<Course> courses, int courseIndex, ScheduleConfiguration config) {
        // Check cancellation
        if (cancelled.get()) {
            return false;
        }

        // Base case: all courses assigned
        if (courseIndex >= courses.size()) {
            return true;
        }

        Course currentCourse = courses.get(courseIndex);

        // Update progress
        updateProgress(courseIndex, courses.size(),
                "Scheduling " + currentCourse.getCourseCode() + "...");

        ExamAssignment assignment = scheduleState.getAssignment(currentCourse.getCourseCode());

        // Try each day and time slot
        for (int day = 0; day < config.getNumDays(); day++) {
            for (int slot = 0; slot < config.getSlotsPerDay(); slot++) {
                if (cancelled.get()) {
                    return false;
                }

                // Get suitable classrooms for this day/slot
                List<Classroom> suitableClassrooms = getSuitableClassrooms(
                        currentCourse, day, slot, scheduleState);

                // Try each classroom
                for (Classroom classroom : suitableClassrooms) {
                    // Temporarily assign
                    assignment.setDay(day);
                    assignment.setTimeSlotIndex(slot);
                    assignment.setClassroomId(classroom.getClassroomId());

                    // Validate assignment
                    ConstraintValidator.ValidationResult validationResult =
                            validator.validateAssignment(assignment, scheduleState);

                    if (validationResult.isValid()) {
                        // Assignment is valid, try to assign remaining courses
                        if (backtrack(scheduleState, courses, courseIndex + 1, config)) {
                            return true; // Success!
                        }
                    }

                    // Backtrack: reset assignment
                    assignment.setDay(-1);
                    assignment.setTimeSlotIndex(-1);
                    assignment.setClassroomId(null);
                }
            }
        }

        // No valid assignment found
        return false;
    }

    /**
     * MRV Heuristic: Order courses by "most constrained first".
     */
    private List<Course> getCoursesOrderedByMRV() {
        List<Course> courses = new ArrayList<>(dataManager.getCourses());

        // Sort by number of enrolled students (descending)
        // More students = more constrained
        courses.sort((c1, c2) -> Integer.compare(
                c2.getEnrolledStudentsCount(),
                c1.getEnrolledStudentsCount()
        ));

        return courses;
    }

    /**
     * Get classrooms suitable for a course at a specific day and time slot.
     */
    private List<Classroom> getSuitableClassrooms(Course course,
                                                  int day,
                                                  int timeSlotIndex,
                                                  ScheduleState scheduleState) {
        List<Classroom> suitable = new ArrayList<>();

        for (Classroom classroom : scheduleState.getAvailableClassrooms()) {
            // Check capacity
            if (classroom.getCapacity() < course.getEnrolledStudentsCount()) {
                continue;
            }

            // Check if classroom is available at this time
            boolean isAvailable = true;
            for (ExamAssignment assignment : scheduleState.getAssignments().values()) {
                if (assignment.isAssigned() &&
                    assignment.getClassroomId().equals(classroom.getClassroomId()) &&
                    assignment.getDay() == day &&
                    assignment.getTimeSlotIndex() == timeSlotIndex) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                suitable.add(classroom);
            }
        }

        // Sort by capacity (prefer smaller classrooms that fit)
        suitable.sort(Comparator.comparingInt(Classroom::getCapacity));

        return suitable;
    }

    /**
     * Cancel the current schedule generation.
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Set progress listener for UI updates.
     */
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Update progress (for UI progress bar).
     */
    private void updateProgress(int current, int total, String message) {
        if (progressListener != null) {
            double progress = total > 0 ? (double) current / total : 0;
            progressListener.onProgress(progress, message);
        }
    }

    /**
     * Interface for progress updates.
     */
    public interface ProgressListener {
        /**
         * Called when progress updates.
         * @param progress Value between 0.0 and 1.0
         * @param message Current status message
         */
        void onProgress(double progress, String message);
    }

    /**
     * Result of schedule generation.
     */
    public static class ScheduleResult {
        private final boolean success;
        private final ScheduleState scheduleState;
        private final String message;
        private final boolean wasCancelled;

        private ScheduleResult(boolean success, ScheduleState scheduleState, String message, boolean wasCancelled) {
            this.success = success;
            this.scheduleState = scheduleState;
            this.message = message;
            this.wasCancelled = wasCancelled;
        }

        public static ScheduleResult success(ScheduleState scheduleState) {
            return new ScheduleResult(true, scheduleState, "Schedule generated successfully", false);
        }

        public static ScheduleResult failure(String message) {
            return new ScheduleResult(false, null, message, false);
        }

        public static ScheduleResult cancelled() {
            return new ScheduleResult(false, null, "Schedule generation was cancelled", true);
        }

        public boolean isSuccess() {
            return success;
        }

        public ScheduleState getScheduleState() {
            return scheduleState;
        }

        public String getMessage() {
            return message;
        }

        public boolean wasCancelled() {
            return wasCancelled;
        }
    }
}
