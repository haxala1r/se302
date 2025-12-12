package org.example.se302.service;

import org.example.se302.model.Classroom;
import org.example.se302.model.Course;
import org.example.se302.model.ImportResult;
import org.example.se302.model.Student;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service class for importing data from CSV files.
 */
public class DataImportService {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("Std_ID_\\d{3}");
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("CourseCode_\\d{2}");
    private static final Pattern CLASSROOM_ID_PATTERN = Pattern.compile("Classroom_\\d{2}");

    private final DataManager dataManager;

    public DataImportService() {
        this.dataManager = DataManager.getInstance();
    }

    /**
     * Import students from CSV file.
     * Format: One student ID per line (after header)
     */
    public ImportResult importStudents(File file) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            if (lines.isEmpty()) {
                result.addError("File is empty");
                return result;
            }

            // Skip header line
            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String studentId = lines.get(i).trim();

                if (studentId.isEmpty()) {
                    continue; // Skip empty lines
                }

                if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
                    result.addError("Line " + (i + 1) + ": Invalid student ID format: " + studentId);
                    continue;
                }

                Student student = new Student(studentId);
                dataManager.addStudent(student);
                count++;
            }

            result.setRecordCount(count);
            if (!result.hasErrors()) {
                result.setSuccess(true);
                result.setMessage("Successfully imported " + count + " students");
            } else {
                result.setMessage("Imported " + count + " students with errors");
            }

        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import courses from CSV file.
     * Format: One course code per line (after header)
     */
    public ImportResult importCourses(File file) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            if (lines.isEmpty()) {
                result.addError("File is empty");
                return result;
            }

            // Skip header line
            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String courseCode = lines.get(i).trim();

                if (courseCode.isEmpty()) {
                    continue; // Skip empty lines
                }

                if (!COURSE_CODE_PATTERN.matcher(courseCode).matches()) {
                    result.addError("Line " + (i + 1) + ": Invalid course code format: " + courseCode);
                    continue;
                }

                Course course = new Course(courseCode);
                dataManager.addCourse(course);
                count++;
            }

            result.setRecordCount(count);
            if (!result.hasErrors()) {
                result.setSuccess(true);
                result.setMessage("Successfully imported " + count + " courses");
            } else {
                result.setMessage("Imported " + count + " courses with errors");
            }

        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import classrooms from CSV file.
     * Format: ClassroomID;Capacity (semicolon-separated)
     */
    public ImportResult importClassrooms(File file) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            if (lines.isEmpty()) {
                result.addError("File is empty");
                return result;
            }

            // Skip header line
            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String classroomLine = lines.get(i).trim();

                if (classroomLine.isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] parts = classroomLine.split(";");
                if (parts.length != 2) {
                    result.addError("Line " + (i + 1) + ": Invalid format. Expected 'ClassroomID;Capacity'");
                    continue;
                }

                String classroomId = parts[0].trim();
                String capacityStr = parts[1].trim();

                if (!CLASSROOM_ID_PATTERN.matcher(classroomId).matches()) {
                    result.addError("Line " + (i + 1) + ": Invalid classroom ID format: " + classroomId);
                    continue;
                }

                try {
                    int capacity = Integer.parseInt(capacityStr);
                    if (capacity <= 0) {
                        result.addError("Line " + (i + 1) + ": Capacity must be positive");
                        continue;
                    }

                    Classroom classroom = new Classroom(classroomId, capacity);
                    dataManager.addClassroom(classroom);
                    count++;

                } catch (NumberFormatException e) {
                    result.addError("Line " + (i + 1) + ": Invalid capacity number: " + capacityStr);
                }
            }

            result.setRecordCount(count);
            if (!result.hasErrors()) {
                result.setSuccess(true);
                result.setMessage("Successfully imported " + count + " classrooms");
            } else {
                result.setMessage("Imported " + count + " classrooms with errors");
            }

        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import enrollment data from CSV file.
     * Format: Alternating lines - course code, then Python list of student IDs,
     * then blank line
     * NO HEADER LINE in this file format.
     */
    public ImportResult importEnrollments(File file) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            if (lines.isEmpty()) {
                result.addError("File is empty");
                return result;
            }

            int courseCount = 0;
            int enrollmentCount = 0;

            // Process lines - pattern is: CourseCode, StudentList, EmptyLine, repeat...
            // NO HEADER in this file format
            int i = 0;
            while (i < lines.size()) {
                String currentLine = lines.get(i).trim();

                // Skip empty lines
                if (currentLine.isEmpty()) {
                    i++;
                    continue;
                }

                // Check if this looks like a course code
                if (COURSE_CODE_PATTERN.matcher(currentLine).matches()) {
                    String courseCode = currentLine;

                    // Verify course exists
                    Course course = dataManager.getCourse(courseCode);
                    if (course == null) {
                        result.addError("Line " + (i + 1) + ": Course not found: " + courseCode);
                        i++;
                        continue;
                    }

                    // Next line should be the student list
                    i++;
                    if (i >= lines.size()) {
                        result.addError("Line " + i + ": Missing student list for course " + courseCode);
                        break;
                    }

                    String studentListStr = lines.get(i).trim();

                    // Check if this is a valid student list (starts with '[')
                    if (!studentListStr.startsWith("[")) {
                        result.addError("Line " + (i + 1) + ": Invalid student list format for course " + courseCode);
                        i++;
                        continue;
                    }

                    // Parse Python list format: ['Std_ID_001', 'Std_ID_002', ...]
                    List<String> studentIds = parseStudentList(studentListStr);

                    for (String studentId : studentIds) {
                        Student student = dataManager.getStudent(studentId);
                        if (student == null) {
                            result.addError("Line " + (i + 1) + ": Student not found: " + studentId);
                            continue;
                        }

                        dataManager.addEnrollment(studentId, courseCode);
                        enrollmentCount++;
                    }

                    courseCount++;
                    i++;
                } else if (currentLine.startsWith("[")) {
                    // This is a student list without a course code before it - skip
                    result.addError("Line " + (i + 1) + ": Student list without course code: " +
                            (currentLine.length() > 50 ? currentLine.substring(0, 50) + "..." : currentLine));
                    i++;
                } else {
                    // Unknown line format - skip
                    i++;
                }
            }

            result.setRecordCount(enrollmentCount);
            if (!result.hasErrors()) {
                result.setSuccess(true);
                result.setMessage(
                        "Successfully imported " + enrollmentCount + " enrollments for " + courseCount + " courses");
            } else {
                result.setMessage("Imported " + enrollmentCount + " enrollments with errors");
            }

        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse a Python-style list of student IDs.
     * Example: ['Std_ID_001', 'Std_ID_002', 'Std_ID_003']
     */
    private List<String> parseStudentList(String pythonList) {
        List<String> studentIds = new ArrayList<>();

        // Remove brackets and split by comma
        if (pythonList.startsWith("[") && pythonList.endsWith("]")) {
            String content = pythonList.substring(1, pythonList.length() - 1);

            // Split by comma and clean up each ID
            String[] parts = content.split(",");
            for (String part : parts) {
                String cleanId = part.trim()
                        .replace("'", "") // Remove single quotes
                        .replace("\"", "") // Remove double quotes
                        .trim();

                if (!cleanId.isEmpty()) {
                    studentIds.add(cleanId);
                }
            }
        }

        return studentIds;
    }
}
