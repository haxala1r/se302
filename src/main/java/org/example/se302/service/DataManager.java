package org.example.se302.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.se302.model.Classroom;
import org.example.se302.model.Course;
import org.example.se302.model.Student;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class that manages all application data.
 * Provides ObservableList collections for UI binding.
 */
public class DataManager {
    private static DataManager instance;

    private ObservableList<Student> students;
    private ObservableList<Course> courses;
    private ObservableList<Classroom> classrooms;

    private Map<String, Student> studentMap;
    private Map<String, Course> courseMap;
    private Map<String, Classroom> classroomMap;

    private DataManager() {
        students = FXCollections.observableArrayList();
        courses = FXCollections.observableArrayList();
        classrooms = FXCollections.observableArrayList();

        studentMap = new HashMap<>();
        courseMap = new HashMap<>();
        classroomMap = new HashMap<>();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // Student operations
    public ObservableList<Student> getStudents() {
        return students;
    }

    public void addStudent(Student student) {
        students.add(student);
        studentMap.put(student.getStudentId(), student);
    }

    public Student getStudent(String studentId) {
        return studentMap.get(studentId);
    }

    public void clearStudents() {
        students.clear();
        studentMap.clear();
    }

    // Course operations
    public ObservableList<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course course) {
        courses.add(course);
        courseMap.put(course.getCourseCode(), course);
    }

    public Course getCourse(String courseCode) {
        return courseMap.get(courseCode);
    }

    public void clearCourses() {
        courses.clear();
        courseMap.clear();
    }

    // Classroom operations
    public ObservableList<Classroom> getClassrooms() {
        return classrooms;
    }

    public void addClassroom(Classroom classroom) {
        classrooms.add(classroom);
        classroomMap.put(classroom.getClassroomId(), classroom);
    }

    public Classroom getClassroom(String classroomId) {
        return classroomMap.get(classroomId);
    }

    public void clearClassrooms() {
        classrooms.clear();
        classroomMap.clear();
    }

    // Utility methods
    public void clearAll() {
        clearStudents();
        clearCourses();
        clearClassrooms();
    }

    public int getTotalStudents() {
        return students.size();
    }

    public int getTotalCourses() {
        return courses.size();
    }

    public int getTotalClassrooms() {
        return classrooms.size();
    }

    public boolean hasData() {
        return !students.isEmpty() || !courses.isEmpty() || !classrooms.isEmpty();
    }

    // Enrollment operations
    public void addEnrollment(String studentId, String courseCode) {
        Student student = getStudent(studentId);
        Course course = getCourse(courseCode);

        if (student != null && course != null) {
            student.addCourse(courseCode);
            course.addStudent(studentId);
        }
    }

    // Schedule Configuration
    private org.example.se302.model.ScheduleConfiguration activeConfiguration;

    public org.example.se302.model.ScheduleConfiguration getActiveConfiguration() {
        return activeConfiguration;
    }

    public void setActiveConfiguration(org.example.se302.model.ScheduleConfiguration activeConfiguration) {
        this.activeConfiguration = activeConfiguration;
    }
}
