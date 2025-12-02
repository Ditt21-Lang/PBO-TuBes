package com.pomodone.repository;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.service.PomodoroService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PomodoroSessionRepository {

    public void insertCompletedSession(long userId, LocalDateTime startedAt, LocalDateTime endedAt, long durationSeconds, PomodoroService.PomodoroMode mode) {
        String sql = """
            INSERT INTO pomodoro_sessions (user_id, started_at, ended_at, duration_seconds, mode, status, created_at)
            VALUES (?, ?, ?, ?, ?::pomodoro_mode, 'COMPLETED', NOW())
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(startedAt));
            pstmt.setTimestamp(3, Timestamp.valueOf(endedAt));
            pstmt.setLong(4, durationSeconds);
            pstmt.setString(5, mode.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal menyimpan sesi pomodoro: " + e.getMessage());
        }
    }

    public int countSessionsSince(long userId, LocalDateTime startInclusive) {
        String sql = """
            SELECT COUNT(*) FROM pomodoro_sessions
            WHERE user_id = ? AND status = 'COMPLETED' AND started_at >= ?
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(startInclusive));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
