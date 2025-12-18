package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for generating exam schedules using Constraint Satisfaction Problem
 * (CSP) solving.
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
     * Uses multi-restart optimization to find the best schedule among multiple attempts.
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

        // Get courses ordered by MRV heuristic
        List<Course> coursesToSchedule = getCoursesOrderedByMRV();

        // Multi-restart optimization: try multiple schedules and pick the best
        int numRestarts = 5; // Generate 5 different schedules
        ScheduleState bestSchedule = null;
        double bestScore = Double.MAX_VALUE;
        int successfulAttempts = 0;

        updateProgress(0, numRestarts, "Generating schedules (multi-restart optimization)...");

        for (int attempt = 0; attempt < numRestarts; attempt++) {
            if (cancelled.get()) {
                return ScheduleResult.cancelled();
            }

            // Create fresh schedule state for this attempt
            ScheduleState scheduleState = initializeScheduleState(config);

            updateProgress(attempt, numRestarts,
                    String.format("Attempt %d/%d: Starting backtracking...", attempt + 1, numRestarts));

            // Randomize search order slightly for diversity
            List<Course> shuffledCourses = new ArrayList<>(coursesToSchedule);
            if (attempt > 0) {
                // Keep MRV heuristic but add small random perturbation
                shuffledCourses = perturbCourseOrder(shuffledCourses, attempt);
            }

            // Run backtracking
            boolean success = backtrack(scheduleState, shuffledCourses, 0, config);

            if (success) {
                successfulAttempts++;

                // Evaluate this schedule using objective function
                double score = ScheduleObjective.evaluateSchedule(scheduleState, config, dataManager);

                updateProgress(attempt + 1, numRestarts,
                        String.format("Attempt %d/%d: Found schedule (score: %.1f, best: %.1f)",
                                attempt + 1, numRestarts, score, bestScore));

                if (score < bestScore) {
                    bestScore = score;
                    bestSchedule = scheduleState.copy();
                }
            } else {
                updateProgress(attempt + 1, numRestarts,
                        String.format("Attempt %d/%d: No valid schedule", attempt + 1, numRestarts));
            }
        }

        if (cancelled.get()) {
            return ScheduleResult.cancelled();
        }

        if (bestSchedule != null) {
            updateProgress(numRestarts, numRestarts,
                    String.format("Complete! Best schedule: score %.1f (%d/%d attempts succeeded)",
                            bestScore, successfulAttempts, numRestarts));
            return ScheduleResult.success(bestSchedule);
        } else {
            return ScheduleResult.failure(
                    "No valid schedule found in " + numRestarts + " attempts. " +
                            "Try increasing days/slots or relaxing constraints.");
        }
    }

    /**
     * Initialize ScheduleState with configuration.
     */
    private ScheduleState initializeScheduleState(ScheduleConfiguration config) {
        ScheduleState state = new ScheduleState();

        // CRITICAL: Set configuration so ScheduleState knows about days/slots
        state.setConfiguration(config);

        // Set available classrooms
        state.setAvailableClassrooms(new ArrayList<>(dataManager.getClassrooms()));

        // Set available time slots based on configuration
        state.setAvailableTimeSlots(config.generateTimeSlots());

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
    private boolean backtrack(ScheduleState scheduleState, List<Course> courses, int courseIndex,
            ScheduleConfiguration config) {
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

        // Get time slots ordered by strategy
        List<DaySlotPair> orderedTimeSlots = getTimeSlotsOrderedByStrategy(config, scheduleState);

        // Try each time slot
        for (DaySlotPair timeSlot : orderedTimeSlots) {
            if (cancelled.get()) {
                return false;
            }

            // Get suitable classrooms for this day/slot (ordered by strategy)
            List<Classroom> suitableClassrooms = getSuitableClassroomsOrdered(
                    currentCourse, timeSlot.day, timeSlot.slot, scheduleState, config);

            // Try each classroom
            for (Classroom classroom : suitableClassrooms) {
                // Temporarily assign
                assignment.setDay(timeSlot.day);
                assignment.setTimeSlotIndex(timeSlot.slot);
                assignment.setClassroomId(classroom.getClassroomId());

                // Validate assignment - pass allowBackToBack from config
                ConstraintValidator.ValidationResult validationResult = validator.validateAssignment(assignment,
                        scheduleState, config.isAllowBackToBackExams());

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
                c1.getEnrolledStudentsCount()));

        return courses;
    }

    /**
     * Add small random perturbation to course order while preserving MRV bias.
     * This creates diverse schedules for multi-restart optimization.
     *
     * @param courses Original course list
     * @param seed    Seed for deterministic randomization
     * @return Perturbed course list
     */
    private List<Course> perturbCourseOrder(List<Course> courses, int seed) {
        Random random = new Random(seed * 1000L); // Deterministic seed for reproducibility
        List<Course> perturbed = new ArrayList<>(courses);

        // Swap 2-3 random pairs (limited perturbation maintains MRV benefits)
        int swaps = 2 + random.nextInt(2);
        for (int i = 0; i < swaps && perturbed.size() > 1; i++) {
            int idx1 = random.nextInt(perturbed.size());
            int idx2 = random.nextInt(perturbed.size());
            Collections.swap(perturbed, idx1, idx2);
        }

        return perturbed;
    }

    /**
     * Get time slots ordered by optimization strategy.
     */
    private List<DaySlotPair> getTimeSlotsOrderedByStrategy(ScheduleConfiguration config, ScheduleState scheduleState) {
        List<DaySlotPair> timeSlots = new ArrayList<>();

        // Generate all day/slot combinations
        for (int day = 0; day < config.getNumDays(); day++) {
            for (int slot = 0; slot < config.getSlotsPerDay(); slot++) {
                timeSlots.add(new DaySlotPair(day, slot));
            }
        }

        // Order based on strategy (deprecated strategies handled in ScheduleObjective)
        switch (config.getOptimizationStrategy()) {
            case MINIMIZE_DAYS:
                // Already in order (day 0 slot 0, day 0 slot 1, ... day 1 slot 0, ...)
                // This fills earlier days first
                break;

            case STUDENT_FRIENDLY:
                // Fill each day completely before moving to next day
                // Within each day, prefer middle slots (avoid early morning)
                // Priority: slots 1 and 2 (middle of day) over slots 0 (early) and 3 (late)
                timeSlots.sort((p1, p2) -> {
                    // Primary: sort by day (fill Day 0 first, then Day 1, etc.)
                    int dayCompare = Integer.compare(p1.day, p2.day);
                    if (dayCompare != 0)
                        return dayCompare;

                    // Secondary: within same day, prefer middle slots
                    // Slot priority order: 1 (best), 2 (good), 0 (early morning), 3 (late afternoon)
                    int[] slotPriority = { 2, 0, 1, 3 }; // Maps slot index to priority (lower is better)
                    int priority1 = p1.slot < slotPriority.length ? slotPriority[p1.slot] : p1.slot;
                    int priority2 = p2.slot < slotPriority.length ? slotPriority[p2.slot] : p2.slot;
                    return Integer.compare(priority1, priority2);
                });
                break;

            default:
                // DEFAULT or others: chronological order
                break;
        }

        return timeSlots;
    }

    /**
     * Get classrooms suitable for a course at a specific day and time slot,
     * ordered according to optimization strategy.
     */
    private List<Classroom> getSuitableClassroomsOrdered(Course course,
            int day,
            int timeSlotIndex,
            ScheduleState scheduleState,
            ScheduleConfiguration config) {
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

        // Order based on strategy (deprecated strategies handled in ScheduleObjective)
        switch (config.getOptimizationStrategy()) {
            case MINIMIZE_CLASSROOMS:
                // Prefer classrooms that are already in use (reuse same classrooms)
                suitable.sort((c1, c2) -> {
                    int usage1 = getClassroomUsageCount(c1.getClassroomId(), scheduleState);
                    int usage2 = getClassroomUsageCount(c2.getClassroomId(), scheduleState);
                    // Sort descending (most used first)
                    return Integer.compare(usage2, usage1);
                });
                break;

            default:
                // DEFAULT: Use round-robin to force distribution across classrooms
                // Always prefer least-used classrooms to ensure multiple classrooms get used
                suitable.sort((c1, c2) -> {
                    int usage1 = getClassroomUsageCount(c1.getClassroomId(), scheduleState);
                    int usage2 = getClassroomUsageCount(c2.getClassroomId(), scheduleState);

                    // Primary: prefer least-used classrooms
                    int usageCompare = Integer.compare(usage1, usage2);
                    if (usageCompare != 0)
                        return usageCompare;

                    // Tiebreaker: prefer smaller capacity (efficient space usage)
                    int capacityCompare = Integer.compare(c1.getCapacity(), c2.getCapacity());
                    if (capacityCompare != 0)
                        return capacityCompare;

                    // Final tiebreaker: classroom ID (deterministic)
                    return c1.getClassroomId().compareTo(c2.getClassroomId());
                });
                break;
        }

        return suitable;
    }

    /**
     * Count how many times a classroom has been used in the current schedule.
     */
    private int getClassroomUsageCount(String classroomId, ScheduleState scheduleState) {
        int count = 0;
        for (ExamAssignment assignment : scheduleState.getAssignments().values()) {
            if (assignment.isAssigned() && assignment.getClassroomId().equals(classroomId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper class to represent a day/slot pair.
     */
    private static class DaySlotPair {
        final int day;
        final int slot;

        DaySlotPair(int day, int slot) {
            this.day = day;
            this.slot = slot;
        }
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
         * 
         * @param progress Value between 0.0 and 1.0
         * @param message  Current status message
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
