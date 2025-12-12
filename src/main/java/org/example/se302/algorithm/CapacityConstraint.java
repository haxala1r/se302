package org.example.se302.algorithm;

import org.example.se302.model.Classroom;
import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ScheduleState;

import java.util.Map;

/**
 * Hard constraint: Classroom capacity must be sufficient for the number of
 * students.
 */
public class CapacityConstraint implements Constraint {

    private Map<String, Classroom> classrooms;

    public CapacityConstraint(Map<String, Classroom> classrooms) {
        this.classrooms = classrooms;
    }

    @Override
    public String getName() {
        return "CAPACITY";
    }

    @Override
    public String getDescription() {
        return "Classroom capacity must be sufficient for the number of enrolled students";
    }

    @Override
    public boolean isSatisfied(ExamAssignment assignment, ScheduleState state) {
        if (!assignment.isAssigned()) {
            return true;
        }

        Classroom classroom = classrooms.get(assignment.getClassroomId());
        if (classroom == null) {
            return false; // Unknown classroom
        }

        return classroom.getCapacity() >= assignment.getStudentCount();
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

        Classroom classroom = classrooms.get(assignment.getClassroomId());
        if (classroom == null) {
            return String.format("Classroom %s does not exist", assignment.getClassroomId());
        }

        return String.format("Classroom %s capacity (%d) is insufficient for %d students",
                assignment.getClassroomId(), classroom.getCapacity(), assignment.getStudentCount());
    }

    public void setClassrooms(Map<String, Classroom> classrooms) {
        this.classrooms = classrooms;
    }
}
