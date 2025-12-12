package org.example.se302.algorithm;

import org.example.se302.model.Classroom;
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleState;

/**
 * Hard constraint: A classroom cannot host two exams at the same time.
 */
public class ClassroomConflictConstraint implements Constraint {

    @Override
    public String getName() {
        return "CLASSROOM_CONFLICT";
    }

    @Override
    public String getDescription() {
        return "A classroom cannot be used for multiple exams at the same time";
    }

    @Override
    public boolean isSatisfied(ExamAssignment assignment, ScheduleState state) {
        if (!assignment.isAssigned()) {
            return true; // Unassigned assignments don't violate constraints
        }

        return state.isClassroomAvailable(assignment.getClassroomId(), assignment.getTimeSlot());
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
        return String.format("Classroom %s is already in use at %s",
                assignment.getClassroomId(), assignment.getTimeSlot());
    }
}
