package org.example.se302.algorithm;

import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleState;

/**
 * Interface for constraints in the CSP-based exam scheduling.
 * Each constraint represents a rule that must be satisfied for a valid
 * schedule.
 */
public interface Constraint {

    /**
     * Gets the name/identifier of this constraint.
     */
    String getName();

    /**
     * Gets a human-readable description of this constraint.
     */
    String getDescription();

    /**
     * Checks if the given assignment satisfies this constraint
     * within the context of the current schedule state.
     * 
     * @param assignment The new assignment to check
     * @param state      The current schedule state
     * @return true if the constraint is satisfied, false otherwise
     */
    boolean isSatisfied(ExamAssignment assignment, ScheduleState state);

    /**
     * Gets the priority/weight of this constraint.
     * Higher values indicate more important constraints.
     * Hard constraints should have very high priority.
     */
    int getPriority();

    /**
     * Checks if this is a hard constraint (must be satisfied)
     * or a soft constraint (preferably satisfied but can be violated).
     */
    boolean isHard();

    /**
     * Gets a detailed message explaining why the constraint was violated
     * for the given assignment.
     * 
     * @param assignment The assignment that violated the constraint
     * @param state      The current schedule state
     * @return A descriptive message, or null if constraint is satisfied
     */
    String getViolationMessage(ExamAssignment assignment, ScheduleState state);
}
