package org.example.se302.data;

import org.example.se302.model.ExamSchedule;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseManager {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String JDBC_URL = "jdbc:sqlite:src/main/resources/db/exam_scheduler.db";

    private static final String CREATE_SCHEDULES_TABLE =
            "CREATE TABLE IF NOT EXISTS schedules (" +
                    "    schedule_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    name TEXT NOT NULL," +
                    "    created_at DATETIME NOT NULL," +
                    "    last_modified DATETIME NOT NULL," +
                    "    is_finalized BOOLEAN NOT NULL DEFAULT 0," +
                    "    config_num_days INTEGER," +
                    "    config_slots_per_day INTEGER," +
                    "    config_start_date TEXT" +
                    ");";

    private static final String CREATE_EXAM_ASSIGNMENTS_TABLE =
            "CREATE TABLE IF NOT EXISTS exam_assignments (" +
                    "    assignment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    schedule_id INTEGER NOT NULL," +
                    "    course_code TEXT NOT NULL," +
                    "    student_count INTEGER NOT NULL," +
                    "    is_locked BOOLEAN NOT NULL DEFAULT 0," +
                    "    day_index INTEGER," +
                    "    timeslot_index INTEGER," +
                    "    classroom_id TEXT," +
                    "    FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE" +
                    ");";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public static void CreateTable() throws SQLException
    {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Schedules tablosunu oluştur
            stmt.execute(CREATE_SCHEDULES_TABLE);

            // 2. Exam Assignments tablosunu oluştur
            stmt.execute(CREATE_EXAM_ASSIGNMENTS_TABLE);

            System.out.println("Veritabanı tabloları başarıyla oluşturuldu veya kontrol edildi.");

        } catch (SQLException e) {
            System.err.println("Veritabanı başlatılırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Veritabanı başlatma başarısız.", e);
        }

    }


}
