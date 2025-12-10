package org.example.se302.model;

/**
 * Represents a classroom in the exam scheduling system.
 */
public class Classroom {
    private String classroomId;
    private int capacity;

    public Classroom(String classroomId, int capacity) {
        this.classroomId = classroomId;
        this.capacity = capacity;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return classroomId + " (Capacity: " + capacity + ")";
    }
}
