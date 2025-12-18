package org.example.se302.service;

import org.example.se302.model.*;

import java.util.*;

/**
 * Calculates quality scores for exam schedules to enable optimization.
 * Lower score = better schedule.
 *
 * Each optimization strategy has its own scoring function that quantifies
 * how well a schedule meets the strategy's goals.
 */
public class ScheduleObjective {

    /**
     * Evaluates a schedule based on the selected optimization strategy.
     *
     * @param state       The schedule state to evaluate
     * @param config      The configuration containing the optimization strategy
     * @param dataManager Data manager for accessing student and course data
     * @return Quality score (lower is better)
     */
    public static double evaluateSchedule(
            ScheduleState state,
            ScheduleConfiguration config,
            DataManager dataManager) {

        // Normalize strategy in case deprecated ones are used
        ScheduleConfiguration.OptimizationStrategy strategy = config.getOptimizationStrategy();

        // Map deprecated strategies to their replacements
        switch (strategy) {
            case BALANCED_DISTRIBUTION:
            case DEFAULT:
                strategy = ScheduleConfiguration.OptimizationStrategy.STUDENT_FRIENDLY;
                break;
            case BALANCE_CLASSROOMS:
                strategy = ScheduleConfiguration.OptimizationStrategy.MINIMIZE_CLASSROOMS;
                break;
            default:
                break;
        }

        // Calculate score based on strategy
        switch (strategy) {
            case MINIMIZE_DAYS:
                return scoreMinimizeDays(state, config);

            case MINIMIZE_CLASSROOMS:
                return scoreMinimizeClassrooms(state);

            case STUDENT_FRIENDLY:
                return scoreStudentFriendly(state, config, dataManager);

            default:
                return scoreStudentFriendly(state, config, dataManager);
        }
    }

    /**
     * Score for MINIMIZE_DAYS strategy.
     * Goal: Pack exams into as few days as possible.
     *
     * @param state  The schedule state
     * @param config The configuration
     * @return Score (lower = fewer days used)
     */
    private static double scoreMinimizeDays(ScheduleState state, ScheduleConfiguration config) {
        // Count number of unique days actually used
        Set<Integer> usedDays = new HashSet<>();
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.isAssigned()) {
                usedDays.add(assignment.getDay());
            }
        }

        // Primary objective: minimize number of days
        // Penalty: 1000 points per day used
        double score = usedDays.size() * 1000.0;

        // Secondary objective: prefer earlier days (Day 0 better than Day 4)
        // This is a tiebreaker when two schedules use the same number of days
        double avgDay = state.getAssignedCoursesList().stream()
                .mapToInt(ExamAssignment::getDay)
                .average()
                .orElse(0.0);
        score += avgDay * 10;

        // Tertiary objective: within each day, fill earlier slots first
        double avgSlot = state.getAssignedCoursesList().stream()
                .mapToInt(ExamAssignment::getTimeSlotIndex)
                .average()
                .orElse(0.0);
        score += avgSlot * 1;

        return score;
    }

    /**
     * Score for MINIMIZE_CLASSROOMS strategy.
     * Goal: Use as few different classrooms as possible.
     *
     * @param state The schedule state
     * @return Score (lower = fewer classrooms used)
     */
    private static double scoreMinimizeClassrooms(ScheduleState state) {
        // Count unique classrooms used
        Set<String> usedClassrooms = new HashSet<>();
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.isAssigned() && assignment.getClassroomId() != null) {
                usedClassrooms.add(assignment.getClassroomId());
            }
        }

        // Primary objective: minimize number of classrooms
        // Penalty: 1000 points per classroom used
        double score = usedClassrooms.size() * 1000.0;

        // Secondary objective: prefer classrooms with lower IDs
        // (e.g., Classroom_01 better than Classroom_10)
        // This is a tiebreaker when two schedules use the same number of classrooms
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.isAssigned() && assignment.getClassroomId() != null) {
                String classroomId = assignment.getClassroomId();
                // Extract numeric part (e.g., "Classroom_01" -> 1)
                try {
                    int num = Integer.parseInt(classroomId.replaceAll("[^0-9]", ""));
                    score += num * 0.1; // Small penalty for higher-numbered classrooms
                } catch (NumberFormatException e) {
                    // If classroom ID doesn't contain number, ignore
                }
            }
        }

        return score;
    }

    /**
     * Score for STUDENT_FRIENDLY strategy.
     * Goal: Minimize gaps in student schedules and create a comfortable exam experience.
     *
     * @param state       The schedule state
     * @param config      The configuration
     * @param dataManager Data manager for accessing student data
     * @return Score (lower = more student-friendly)
     */
    private static double scoreStudentFriendly(
            ScheduleState state,
            ScheduleConfiguration config,
            DataManager dataManager) {

        double score = 0.0;

        // Calculate total gaps in student schedules
        for (Student student : dataManager.getStudents()) {
            List<ExamAssignment> studentExams = getStudentExams(student, state, dataManager);

            if (studentExams.isEmpty()) {
                continue;
            }

            // Sort exams by day, then by time slot
            studentExams.sort(Comparator
                    .comparingInt(ExamAssignment::getDay)
                    .thenComparingInt(ExamAssignment::getTimeSlotIndex));

            // Calculate gaps (empty slots between exams on the same day)
            for (int i = 1; i < studentExams.size(); i++) {
                ExamAssignment prev = studentExams.get(i - 1);
                ExamAssignment curr = studentExams.get(i);

                if (prev.getDay() == curr.getDay()) {
                    int gap = curr.getTimeSlotIndex() - prev.getTimeSlotIndex() - 1;
                    if (gap > 0) {
                        // Penalty: 10 points per gap slot
                        // Example: Exam at slot 0 and slot 3 = 2 gap slots = 20 points penalty
                        score += gap * 10.0;
                    }
                }
            }

            // Penalty for early-morning exams (first slot of the day)
            // Students prefer later exam times
            for (ExamAssignment exam : studentExams) {
                if (exam.getTimeSlotIndex() == 0) {
                    score += 5.0; // Small penalty for first slot (typically 8-9am)
                }
            }

            // Penalty for late-afternoon exams (last slot of the day)
            int lastSlot = config.getSlotsPerDay() - 1;
            for (ExamAssignment exam : studentExams) {
                if (exam.getTimeSlotIndex() == lastSlot) {
                    score += 3.0; // Smaller penalty for last slot
                }
            }

            // Bonus for clustered exam days (exams on consecutive days are harder)
            // Prefer spreading exams across non-consecutive days when possible
            Set<Integer> examDays = new HashSet<>();
            for (ExamAssignment exam : studentExams) {
                examDays.add(exam.getDay());
            }
            List<Integer> sortedDays = new ArrayList<>(examDays);
            Collections.sort(sortedDays);

            for (int i = 1; i < sortedDays.size(); i++) {
                if (sortedDays.get(i) - sortedDays.get(i - 1) == 1) {
                    // Consecutive days
                    score += 2.0; // Small penalty for exams on consecutive days
                }
            }
        }

        // Secondary objective: balance classroom usage
        // Prefer schedules that distribute exams across classrooms
        Map<String, Integer> classroomUsage = new HashMap<>();
        for (ExamAssignment assignment : state.getAssignments().values()) {
            if (assignment.isAssigned()) {
                classroomUsage.merge(assignment.getClassroomId(), 1, Integer::sum);
            }
        }

        // Calculate standard deviation of classroom usage
        if (!classroomUsage.isEmpty()) {
            double avgUsage = classroomUsage.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            double variance = classroomUsage.values().stream()
                    .mapToDouble(usage -> Math.pow(usage - avgUsage, 2))
                    .average()
                    .orElse(0.0);

            double stdDev = Math.sqrt(variance);
            // Penalty for unbalanced classroom usage
            score += stdDev * 2.0;
        }

        return score;
    }

    /**
     * Gets all exam assignments for a specific student.
     *
     * @param student     The student
     * @param state       The schedule state
     * @param dataManager Data manager for course lookup
     * @return List of exam assignments for this student
     */
    private static List<ExamAssignment> getStudentExams(
            Student student,
            ScheduleState state,
            DataManager dataManager) {

        List<ExamAssignment> exams = new ArrayList<>();
        for (String courseCode : student.getEnrolledCourses()) {
            ExamAssignment assignment = state.getAssignment(courseCode);
            if (assignment != null && assignment.isAssigned()) {
                exams.add(assignment);
            }
        }
        return exams;
    }
}
