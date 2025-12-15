package org.example.se302.data;

import org.example.se302.model.ExamAssignment;
import org.example.se302.model.ExamSchedule;
import org.example.se302.model.ScheduleConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScheduleDAO {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void InsertSchedule(ExamSchedule examSchedule) throws SQLException
    {
        long scheduleId = 0;

        try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);

        String sqlSchedule = "INSERT INTO schedules (name, created_at, last_modified, is_finalized, " +
                "config_num_days, config_slots_per_day, config_start_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sqlSchedule, Statement.RETURN_GENERATED_KEYS)) {

            String createdAtStr = FORMATTER.format(examSchedule.getCreatedAt());
            String modifiedAtStr = FORMATTER.format(examSchedule.getLastModified());

            stmt.setString(1, examSchedule.getScheduleName());
            stmt.setString(2, createdAtStr);
            stmt.setString(3, modifiedAtStr);
            stmt.setBoolean(4, examSchedule.isFinalized());

            stmt.setInt(5, examSchedule.getConfiguration().getNumDays());
            stmt.setInt(6, examSchedule.getConfiguration().getSlotsPerDay());
            stmt.setString(7, examSchedule.getConfiguration().getStartDate().toString());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    scheduleId = rs.getLong(1);
                } else {
                    throw new SQLException("Program ID'si eklenemedi, veritabanı hatası.");
                }
            }
        }

        if (scheduleId > 0) {
            InsertAssignments(conn, scheduleId, examSchedule.getAllAssignments());
        }

        conn.commit();

        } catch (SQLException e) {
            System.err.println("Yeni program kaydedilirken bir veritabanı hatası oluştu: " + e.getMessage());
            throw e;
        }
    }

    private static void InsertAssignments(Connection conn, long scheduleId, Collection<ExamAssignment> assignments) throws SQLException {
        String sql = "INSERT INTO exam_assignments (schedule_id, course_code, student_count, is_locked, day_index, timeslot_index, classroom_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ExamAssignment assignment : assignments) {

                stmt.setLong(1, scheduleId);
                stmt.setString(2, assignment.getCourseCode());
                stmt.setInt(3, assignment.getStudentCount());
                stmt.setBoolean(4, assignment.isLocked());

                stmt.setInt(5, assignment.getDay());
                stmt.setInt(6, assignment.getTimeSlotIndex());

                if (assignment.getClassroomId() != null) {
                    stmt.setString(7, assignment.getClassroomId());
                } else {
                    stmt.setNull(7, Types.VARCHAR);
                }

                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public static boolean DeleteSchedule(long scheduleId) throws SQLException
    {
        String sql = "DELETE FROM schedules WHERE schedule_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, scheduleId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.printf("Schedule ID %d başarıyla silindi.\n", scheduleId);
                return true;
            } else {
                System.out.printf("Hata: Schedule ID %d bulunamadı veya silinemedi.\n", scheduleId);
                return false;
            }

        } catch (SQLException e) {
            System.err.printf("Program ID %d silinirken veritabanı hatası oluştu: %s\n", scheduleId, e.getMessage());
            throw e;
        }
    }
    public ExamSchedule loadSchedule(long scheduleId) throws SQLException {
        ScheduleConfiguration config = null;
        ExamSchedule schedule = null;

        try (Connection conn = DatabaseManager.getConnection()) {

            // 1. Program Meta Verisini ve Konfigürasyonu Yükle
            config = loadConfiguration(conn, scheduleId);

            if (config == null) {
                return null; // Belirtilen ID'ye sahip program bulunamadı.
            }

            // 2. Temel Schedule Nesnesini Oluştur
            schedule = loadScheduleMetadata(conn, scheduleId, config);

            if (schedule != null) {
                // 3. Atamaları Yükle ve Programa Ekle
                List<ExamAssignment> assignments = loadAssignments(conn, scheduleId);

                for (ExamAssignment assignment : assignments) {
                    // ExamSchedule'ın assignments Map'ine bu atamaları ekle
                    schedule.addAssignment(assignment);
                }
            }

            return schedule;

        } catch (SQLException e) {
            System.err.printf("Program ID %d yüklenirken veritabanı hatası oluştu: %s\n", scheduleId, e.getMessage());
            throw e;
        }
    }

    private ScheduleConfiguration loadConfiguration(Connection conn, long scheduleId) throws SQLException {
        String sql = "SELECT config_num_days, config_slots_per_day, config_start_date FROM schedules WHERE schedule_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int numDays = rs.getInt("config_num_days");
                    int slotsPerDay = rs.getInt("config_slots_per_day");
                    String startDateStr = rs.getString("config_start_date");

                    // Not: ScheduleConfiguration constructor'ınızın bu alanları almasını varsayıyorum.
                    return new ScheduleConfiguration(numDays, slotsPerDay);
                }
            }
        }
        return null;
    }


    private ExamSchedule loadScheduleMetadata(Connection conn, long scheduleId, ScheduleConfiguration config) throws SQLException {
        String sql = "SELECT name, created_at, last_modified, is_finalized FROM schedules WHERE schedule_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"), FORMATTER);
                    LocalDateTime lastModified = LocalDateTime.parse(rs.getString("last_modified"), FORMATTER);
                    boolean isFinalized = rs.getBoolean("is_finalized");

                    // ExamSchedule'ı konfigürasyon ile başlat
                    ExamSchedule schedule = new ExamSchedule(config);

                    schedule.setScheduleName(name);
                    schedule.setFinalized(isFinalized);

                    return schedule;
                }
            }
        }
        return null;
    }

    private List<ExamAssignment> loadAssignments(Connection conn, long scheduleId) throws SQLException {
        List<ExamAssignment> assignments = new ArrayList<>();
        String sql = "SELECT course_code, student_count, is_locked, day_index, timeslot_index, classroom_id " +
                "FROM exam_assignments WHERE schedule_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String courseCode = rs.getString("course_code");
                    int studentCount = rs.getInt("student_count");
                    boolean isLocked = rs.getBoolean("is_locked");
                    int dayIndex = rs.getInt("day_index");
                    int timeSlotIndex = rs.getInt("timeslot_index");
                    String classroomId = rs.getString("classroom_id");

                    ExamAssignment assignment = new ExamAssignment(courseCode);
                    assignment.setStudentCount(studentCount);
                    assignment.setLocked(isLocked);

                    assignment.setDay(dayIndex);
                    assignment.setTimeSlotIndex(timeSlotIndex);
                    assignment.setClassroomId(classroomId);

                    assignments.add(assignment);
                }
            }
        }
        return assignments;
    }





}
