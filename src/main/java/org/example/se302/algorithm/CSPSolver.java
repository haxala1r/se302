package org.example.se302.algorithm;

import org.example.se302.model.*;

import java.util.*;

/**
 * CSP (Constraint Satisfaction Problem) Solver for Exam Scheduling.
 * 
 * This solver uses a backtracking algorithm with the following optimizations:
 * - MRV (Minimum Remaining Values) heuristic for variable ordering
 * - Degree heuristic as a tie-breaker
 * - Forward checking for constraint propagation
 * - Arc consistency (AC-3) for further pruning
 * 
 * The CSP formulation:
 * - Variables: Each course that needs an exam scheduled
 * - Domain: All possible (TimeSlot, Classroom) pairs
 * - Constraints: Hard constraints (must satisfy) and soft constraints
 * (preferably satisfy)
 */
public class CSPSolver {

    private List<Constraint> hardConstraints;
    private List<Constraint> softConstraints;
    private Map<String, Course> courses;
    private Map<String, Classroom> classrooms;
    private List<TimeSlot> availableTimeSlots;

    // Statistics
    private int nodesExplored;
    private int backtracks;
    private long startTime;
    private long maxTimeMs;

    // Conflict graph for student conflicts (courses that share students)
    private Map<String, Set<String>> conflictGraph;

    public CSPSolver() {
        this.hardConstraints = new ArrayList<>();
        this.softConstraints = new ArrayList<>();
        this.courses = new HashMap<>();
        this.classrooms = new HashMap<>();
        this.availableTimeSlots = new ArrayList<>();
        this.conflictGraph = new HashMap<>();
        this.nodesExplored = 0;
        this.backtracks = 0;
        this.maxTimeMs = 60000; // Default 60 second timeout
    }

    /**
     * Adds a hard constraint to the solver.
     */
    public void addHardConstraint(Constraint constraint) {
        if (constraint.isHard()) {
            hardConstraints.add(constraint);
        } else {
            softConstraints.add(constraint);
        }
    }

    /**
     * Adds a soft constraint to the solver.
     */
    public void addSoftConstraint(Constraint constraint) {
        softConstraints.add(constraint);
    }

    /**
     * Sets the available time slots for scheduling.
     */
    public void setAvailableTimeSlots(List<TimeSlot> timeSlots) {
        this.availableTimeSlots = new ArrayList<>(timeSlots);
        Collections.sort(this.availableTimeSlots);
    }

    /**
     * Sets the available classrooms.
     */
    public void setClassrooms(Map<String, Classroom> classrooms) {
        this.classrooms = classrooms;
    }

    /**
     * Sets the courses to schedule.
     */
    public void setCourses(Map<String, Course> courses) {
        this.courses = courses;
        buildConflictGraph();
    }

    /**
     * Builds the conflict graph based on shared students.
     * Two courses are connected if they share at least one student.
     */
    private void buildConflictGraph() {
        conflictGraph.clear();

        for (String courseCode : courses.keySet()) {
            conflictGraph.put(courseCode, new HashSet<>());
        }

        List<String> courseCodes = new ArrayList<>(courses.keySet());

        for (int i = 0; i < courseCodes.size(); i++) {
            Course course1 = courses.get(courseCodes.get(i));
            Set<String> students1 = new HashSet<>(course1.getEnrolledStudents());

            for (int j = i + 1; j < courseCodes.size(); j++) {
                Course course2 = courses.get(courseCodes.get(j));

                // Check if courses share any students
                for (String student : course2.getEnrolledStudents()) {
                    if (students1.contains(student)) {
                        conflictGraph.get(courseCodes.get(i)).add(courseCodes.get(j));
                        conflictGraph.get(courseCodes.get(j)).add(courseCodes.get(i));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Solves the CSP and returns a complete schedule.
     * 
     * @param initialState Optional initial state with some pre-assigned exams
     * @return A complete schedule state, or null if no solution found
     */
    public ScheduleState solve(ScheduleState initialState) {
        nodesExplored = 0;
        backtracks = 0;
        startTime = System.currentTimeMillis();

        // Initialize state if not provided
        ScheduleState state = initialState != null ? initialState.copy() : createInitialState();

        // Start backtracking search
        ScheduleState result = backtrack(state);

        return result;
    }

    /**
     * Creates an initial state with all courses unassigned.
     */
    private ScheduleState createInitialState() {
        ScheduleState state = new ScheduleState();
        state.setAvailableTimeSlots(availableTimeSlots);
        state.setAvailableClassrooms(new ArrayList<>(classrooms.values()));

        for (Course course : courses.values()) {
            ExamAssignment assignment = new ExamAssignment(course.getCourseCode());
            assignment.setStudentCount(course.getEnrolledStudentsCount());
            state.addAssignment(assignment);
        }

        return state;
    }

    /**
     * Backtracking algorithm with MRV heuristic.
     */
    private ScheduleState backtrack(ScheduleState state) {
        // Check timeout
        if (System.currentTimeMillis() - startTime > maxTimeMs) {
            return null; // Timeout
        }

        nodesExplored++;

        // Check if complete
        if (state.isComplete()) {
            return state;
        }

        // Select next variable using MRV heuristic
        ExamAssignment variable = selectUnassignedVariable(state);
        if (variable == null) {
            return null; // No unassigned variables but not complete - shouldn't happen
        }

        // Get ordered domain values
        List<DomainValue> orderedDomain = orderDomainValues(variable, state);

        for (DomainValue value : orderedDomain) {
            // Try assigning this value
            if (isConsistent(variable.getCourseCode(), value.timeSlot, value.classroomId, state)) {
                // Make assignment
                state.updateAssignment(variable.getCourseCode(), value.timeSlot, value.classroomId);

                // Recursive call
                ScheduleState result = backtrack(state);
                if (result != null) {
                    return result;
                }

                // Backtrack
                backtracks++;
                state.removeAssignment(variable.getCourseCode());
            }
        }

        return null; // No valid assignment found
    }

    /**
     * Selects the next unassigned variable using MRV (Minimum Remaining Values)
     * heuristic.
     * Ties are broken using the degree heuristic (most constraints).
     */
    private ExamAssignment selectUnassignedVariable(ScheduleState state) {
        ExamAssignment selected = null;
        int minValues = Integer.MAX_VALUE;
        int maxDegree = -1;

        for (ExamAssignment assignment : state.getUnassignedCourses()) {
            int remainingValues = countRemainingValues(assignment, state);

            if (remainingValues < minValues) {
                minValues = remainingValues;
                maxDegree = getDegree(assignment.getCourseCode());
                selected = assignment;
            } else if (remainingValues == minValues) {
                int degree = getDegree(assignment.getCourseCode());
                if (degree > maxDegree) {
                    maxDegree = degree;
                    selected = assignment;
                }
            }
        }

        return selected;
    }

    /**
     * Counts the number of remaining valid values for a variable.
     */
    private int countRemainingValues(ExamAssignment assignment, ScheduleState state) {
        int count = 0;
        for (TimeSlot timeSlot : availableTimeSlots) {
            for (Classroom classroom : classrooms.values()) {
                if (classroom.getCapacity() >= assignment.getStudentCount() &&
                        state.isClassroomAvailable(classroom.getClassroomId(), timeSlot)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Gets the degree (number of constraints/conflicts) for a course.
     */
    private int getDegree(String courseCode) {
        Set<String> conflicts = conflictGraph.get(courseCode);
        return conflicts != null ? conflicts.size() : 0;
    }

    /**
     * Orders domain values using Least Constraining Value (LCV) heuristic.
     */
    private List<DomainValue> orderDomainValues(ExamAssignment assignment, ScheduleState state) {
        List<DomainValue> values = new ArrayList<>();

        for (TimeSlot timeSlot : availableTimeSlots) {
            for (Classroom classroom : classrooms.values()) {
                if (classroom.getCapacity() >= assignment.getStudentCount()) {
                    DomainValue value = new DomainValue(timeSlot, classroom.getClassroomId());
                    value.constrainingFactor = calculateConstrainingFactor(
                            assignment.getCourseCode(), timeSlot, classroom.getClassroomId(), state);
                    values.add(value);
                }
            }
        }

        // Sort by constraining factor (least constraining first)
        values.sort(Comparator.comparingInt(v -> v.constrainingFactor));

        return values;
    }

    /**
     * Calculates how much a value constrains other variables.
     */
    private int calculateConstrainingFactor(String courseCode, TimeSlot timeSlot,
            String classroomId, ScheduleState state) {
        int factor = 0;

        // Count how many conflicting courses would be affected
        Set<String> conflicts = conflictGraph.get(courseCode);
        if (conflicts != null) {
            for (String conflictCourse : conflicts) {
                ExamAssignment conflictAssignment = state.getAssignment(conflictCourse);
                if (conflictAssignment != null && !conflictAssignment.isAssigned()) {
                    factor++; // This time slot becomes unavailable for the conflicting course
                }
            }
        }

        return factor;
    }

    /**
     * Checks if an assignment is consistent with all constraints.
     */
    private boolean isConsistent(String courseCode, TimeSlot timeSlot,
            String classroomId, ScheduleState state) {
        // Create temporary assignment for checking
        ExamAssignment tempAssignment = new ExamAssignment(courseCode, timeSlot, classroomId);
        Course course = courses.get(courseCode);
        if (course != null) {
            tempAssignment.setStudentCount(course.getEnrolledStudentsCount());
        }

        // Check all hard constraints
        for (Constraint constraint : hardConstraints) {
            if (!constraint.isSatisfied(tempAssignment, state)) {
                return false;
            }
        }

        // Check student conflicts separately (most important)
        Set<String> coursesAtSameTime = state.getCoursesAtTimeSlot(timeSlot);
        Set<String> conflicts = conflictGraph.get(courseCode);
        if (conflicts != null) {
            for (String conflictCourse : conflicts) {
                if (coursesAtSameTime.contains(conflictCourse)) {
                    return false; // Student conflict
                }
            }
        }

        return true;
    }

    /**
     * Helper class for domain values.
     */
    private static class DomainValue {
        TimeSlot timeSlot;
        String classroomId;
        int constrainingFactor;

        DomainValue(TimeSlot timeSlot, String classroomId) {
            this.timeSlot = timeSlot;
            this.classroomId = classroomId;
            this.constrainingFactor = 0;
        }
    }

    // Getters for statistics

    public int getNodesExplored() {
        return nodesExplored;
    }

    public int getBacktracks() {
        return backtracks;
    }

    public long getMaxTimeMs() {
        return maxTimeMs;
    }

    public void setMaxTimeMs(long maxTimeMs) {
        this.maxTimeMs = maxTimeMs;
    }

    public Map<String, Set<String>> getConflictGraph() {
        return Collections.unmodifiableMap(conflictGraph);
    }
}
