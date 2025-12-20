package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for generating exam schedules using a greedy constraint-based
 * algorithm.
 * 
 * Constraints enforced:
 * 1. No student can have two exams at the same time
 * 2. No student can have more than 2 exams per day
 * 3. Students need at least 1 hour break between consecutive exams
 * 4. Number of concurrent exams cannot exceed available classrooms
 * 
 * Features:
 * - Multiple exams can be scheduled in the same time slot (in different
 * classrooms)
 * - Timeout protection (30 seconds max)
 * - Clear error messages on failure
 */
public class ScheduleGeneratorService {
    private final DataManager dataManager;
    private final AtomicBoolean cancelled;
    private ProgressListener progressListener;

    // Timeout in milliseconds (30 seconds)
    private static final long TIMEOUT_MS = 30000;

    // Minimum break between exams in slots (1 hour = 1 slot typically)
    private static final int MIN_BREAK_SLOTS = 1;

    // Maximum exams per student per day
    private static final int MAX_EXAMS_PER_DAY = 2;

    public ScheduleGeneratorService() {
        this.dataManager = DataManager.getInstance();
        this.cancelled = new AtomicBoolean(false);
    }

    /**
     * Generate an exam schedule based on configuration.
     * Uses greedy algorithm with conflict detection.
     */
    public ScheduleResult generateSchedule(ScheduleConfiguration config) {
        cancelled.set(false);
        long startTime = System.currentTimeMillis();

        // Validate input
        if (dataManager.getTotalCourses() == 0) {
            return ScheduleResult.failure("No courses to schedule. Please import course data first.");
        }

        if (dataManager.getTotalClassrooms() == 0) {
            return ScheduleResult.failure("No classrooms available. Please import classroom data first.");
        }

        List<Course> courses = new ArrayList<>(dataManager.getCourses());
        List<Classroom> classrooms = new ArrayList<>(dataManager.getClassrooms());

        // Build conflict graph: which courses share students
        Map<String, Set<String>> conflictGraph = buildConflictGraph(courses);

        // Sort courses by number of conflicts (most constrained first)
        courses.sort((c1, c2) -> {
            int conflicts1 = conflictGraph.getOrDefault(c1.getCourseCode(), Collections.emptySet()).size();
            int conflicts2 = conflictGraph.getOrDefault(c2.getCourseCode(), Collections.emptySet()).size();
            if (conflicts1 != conflicts2) {
                return Integer.compare(conflicts2, conflicts1); // More conflicts first
            }
            // Tiebreaker: more students first
            return Integer.compare(c2.getEnrolledStudentsCount(), c1.getEnrolledStudentsCount());
        });

        // Initialize schedule state
        ScheduleState scheduleState = initializeScheduleState(config);

        // Track student exam assignments: studentId -> list of (day, slot) pairs
        Map<String, List<int[]>> studentExams = new HashMap<>();

        // Track classroom usage: "day-slot" -> set of used classroom IDs
        Map<String, Set<String>> slotClassrooms = new HashMap<>();

        int totalCourses = courses.size();
        int scheduledCount = 0;

        updateProgress(0, totalCourses, "Starting schedule generation...");

        // Try to schedule each course
        for (int i = 0; i < courses.size(); i++) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return ScheduleResult.failure(
                        String.format("Scheduling timed out after 30 seconds. Scheduled %d/%d courses. " +
                                "Try increasing the number of days or time slots.", scheduledCount, totalCourses));
            }

            // Check cancellation
            if (cancelled.get()) {
                return ScheduleResult.cancelled();
            }

            Course course = courses.get(i);
            List<String> enrolledStudentsList = course.getEnrolledStudents();
            Set<String> enrolledStudents = new HashSet<>(enrolledStudentsList);

            updateProgress(i, totalCourses, "Scheduling " + course.getCourseCode() + "...");

            // Find a valid slot for this course
            boolean assigned = false;
            String failureReason = "";

            // Try each day and slot
            dayLoop: for (int day = 0; day < config.getNumDays() && !assigned; day++) {
                for (int slot = 0; slot < config.getSlotsPerDay() && !assigned; slot++) {
                    // Check if this slot is valid for all enrolled students
                    String slotKey = day + "-" + slot;
                    boolean slotValid = true;
                    String reason = "";

                    for (String studentId : enrolledStudents) {
                        // Check constraint 1: No concurrent exams
                        List<int[]> studentExamList = studentExams.getOrDefault(studentId, new ArrayList<>());
                        for (int[] exam : studentExamList) {
                            if (exam[0] == day && exam[1] == slot) {
                                slotValid = false;
                                reason = "Student " + studentId + " already has exam at this time";
                                break;
                            }
                        }
                        if (!slotValid)
                            break;

                        // Check constraint 2: Max 2 exams per day
                        int examsToday = 0;
                        for (int[] exam : studentExamList) {
                            if (exam[0] == day)
                                examsToday++;
                        }
                        if (examsToday >= MAX_EXAMS_PER_DAY) {
                            slotValid = false;
                            reason = "Student " + studentId + " already has " + MAX_EXAMS_PER_DAY + " exams on day "
                                    + (day + 1);
                            break;
                        }

                        // Check constraint 3: At least 1 hour break between exams
                        for (int[] exam : studentExamList) {
                            if (exam[0] == day) {
                                int slotDiff = Math.abs(exam[1] - slot);
                                if (slotDiff > 0 && slotDiff <= MIN_BREAK_SLOTS) {
                                    slotValid = false;
                                    reason = "Student " + studentId + " needs at least 1 hour break between exams";
                                    break;
                                }
                            }
                        }
                        if (!slotValid)
                            break;
                    }

                    if (!slotValid) {
                        failureReason = reason;
                        continue;
                    }

                    // Check constraint 4: Find available classroom
                    Set<String> usedClassrooms = slotClassrooms.getOrDefault(slotKey, new HashSet<>());
                    Classroom selectedClassroom = null;

                    // Sort classrooms by capacity (prefer smallest that fits)
                    List<Classroom> sortedClassrooms = new ArrayList<>(classrooms);
                    sortedClassrooms.sort(Comparator.comparingInt(Classroom::getCapacity));

                    for (Classroom classroom : sortedClassrooms) {
                        if (!usedClassrooms.contains(classroom.getClassroomId()) &&
                                classroom.getCapacity() >= course.getEnrolledStudentsCount()) {
                            selectedClassroom = classroom;
                            break;
                        }
                    }

                    if (selectedClassroom == null) {
                        failureReason = "No available classroom with sufficient capacity at day " + (day + 1) + " slot "
                                + (slot + 1);
                        continue;
                    }

                    // All constraints satisfied - assign the exam
                    ExamAssignment assignment = scheduleState.getAssignment(course.getCourseCode());
                    scheduleState.updateAssignment(
                            assignment.getCourseCode(),
                            day,
                            slot,
                            selectedClassroom.getClassroomId());

                    // Update tracking structures
                    for (String studentId : enrolledStudents) {
                        studentExams.computeIfAbsent(studentId, k -> new ArrayList<>())
                                .add(new int[] { day, slot });
                    }

                    usedClassrooms.add(selectedClassroom.getClassroomId());
                    slotClassrooms.put(slotKey, usedClassrooms);

                    assigned = true;
                    scheduledCount++;
                }
            }

            if (!assigned) {
                return ScheduleResult.failure(
                        String.format("Could not schedule course %s. %s. " +
                                "Scheduled %d/%d courses before failure. " +
                                "Try increasing days/slots or reducing course conflicts.",
                                course.getCourseCode(), failureReason, scheduledCount, totalCourses));
            }
        }

        // Validate final schedule
        int violations = validateSchedule(scheduleState, studentExams);
        scheduleState.setConstraintViolations(violations);

        updateProgress(totalCourses, totalCourses,
                String.format("Complete! Scheduled all %d courses.", totalCourses));

        return ScheduleResult.success(scheduleState);
    }

    /**
     * Build conflict graph - courses that share students cannot be scheduled at the
     * same time.
     */
    private Map<String, Set<String>> buildConflictGraph(List<Course> courses) {
        Map<String, Set<String>> conflictGraph = new HashMap<>();

        // Map student to their courses
        Map<String, Set<String>> studentCourses = new HashMap<>();
        for (Course course : courses) {
            for (String studentId : course.getEnrolledStudents()) {
                studentCourses.computeIfAbsent(studentId, k -> new HashSet<>())
                        .add(course.getCourseCode());
            }
        }

        // Build conflict edges
        for (Set<String> coursesOfStudent : studentCourses.values()) {
            if (coursesOfStudent.size() > 1) {
                List<String> courseList = new ArrayList<>(coursesOfStudent);
                for (int i = 0; i < courseList.size(); i++) {
                    for (int j = i + 1; j < courseList.size(); j++) {
                        String c1 = courseList.get(i);
                        String c2 = courseList.get(j);
                        conflictGraph.computeIfAbsent(c1, k -> new HashSet<>()).add(c2);
                        conflictGraph.computeIfAbsent(c2, k -> new HashSet<>()).add(c1);
                    }
                }
            }
        }

        return conflictGraph;
    }

    /**
     * Initialize ScheduleState with configuration.
     */
    private ScheduleState initializeScheduleState(ScheduleConfiguration config) {
        ScheduleState state = new ScheduleState();

        // Set configuration so ScheduleState knows about days/slots
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
     * Validate the final schedule and count any violations.
     */
    private int validateSchedule(ScheduleState scheduleState, Map<String, List<int[]>> studentExams) {
        int violations = 0;

        for (Map.Entry<String, List<int[]>> entry : studentExams.entrySet()) {
            List<int[]> exams = entry.getValue();

            // Group exams by day
            Map<Integer, List<Integer>> examsByDay = new HashMap<>();
            for (int[] exam : exams) {
                examsByDay.computeIfAbsent(exam[0], k -> new ArrayList<>()).add(exam[1]);
            }

            for (Map.Entry<Integer, List<Integer>> dayEntry : examsByDay.entrySet()) {
                List<Integer> slots = dayEntry.getValue();

                // Check max 2 exams per day
                if (slots.size() > MAX_EXAMS_PER_DAY) {
                    violations++;
                }

                // Check for concurrent exams (should not happen, but validate)
                Set<Integer> uniqueSlots = new HashSet<>(slots);
                if (uniqueSlots.size() != slots.size()) {
                    violations++;
                }

                // Check break time
                Collections.sort(slots);
                for (int i = 1; i < slots.size(); i++) {
                    if (slots.get(i) - slots.get(i - 1) <= MIN_BREAK_SLOTS) {
                        violations++;
                    }
                }
            }
        }

        return violations;
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
