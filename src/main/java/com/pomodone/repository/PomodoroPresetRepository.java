package com.pomodone.repository;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PomodoroPresetRepository {
    private static final Logger log = LoggerFactory.getLogger(PomodoroPresetRepository.class);

    public CustomPomodoroPreset findLatestByUser(long userId) {
        String sql = """
            SELECT id, user_id, preset_name, focus_minutes, short_break_minutes, long_break_minutes, rounds
            FROM pomodoro_custom_presets
            WHERE user_id = ?
            ORDER BY updated_at DESC
            LIMIT 1
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Gagal mengambil preset custom user {}", userId, e);
        }
        return null;
    }

    public void upsert(long userId, CustomPomodoroPreset preset) {
        String sql = """
            INSERT INTO pomodoro_custom_presets (user_id, preset_name, focus_minutes, short_break_minutes, long_break_minutes, rounds)
            VALUES (?, ?, ?, ?, ?, ?)
            -- Upsert portable: ON CONFLICT ada di Postgres/SQLite modern
            ON CONFLICT (user_id, preset_name) DO UPDATE SET
                focus_minutes = EXCLUDED.focus_minutes,
                short_break_minutes = EXCLUDED.short_break_minutes,
                long_break_minutes = EXCLUDED.long_break_minutes,
                rounds = EXCLUDED.rounds,
                updated_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, preset.getPresetName());
            pstmt.setInt(3, preset.getFocusMinutes());
            pstmt.setInt(4, preset.getShortBreakMinutes());
            pstmt.setInt(5, preset.getLongBreakMinutes());
            pstmt.setInt(6, preset.getRounds());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Gagal upsert preset custom user {}", userId, e);
            throw new RuntimeException("Gagal menyimpan custom pomodoro preset: " + e.getMessage(), e);
        }
    }

    private CustomPomodoroPreset mapRow(ResultSet rs) throws SQLException {
        return new CustomPomodoroPreset(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("preset_name"),
                rs.getInt("focus_minutes"),
                rs.getInt("short_break_minutes"),
                rs.getInt("long_break_minutes"),
                rs.getInt("rounds")
        );
    }
}
