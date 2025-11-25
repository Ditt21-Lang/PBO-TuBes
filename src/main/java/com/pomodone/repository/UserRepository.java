package com.pomodone.repository;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("daily_pomodoro_target"),
                        rs.getInt("weekly_pomodoro_target")
                );
                return Optional.of(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // In a real app, use a logger
        }
        return Optional.empty();
    }

    public boolean updateName(long id, String name) {
        String sql = "UPDATE users SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setLong(2, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTargets(long id, int dailyTarget, int weeklyTarget) {
        String sql = "UPDATE users SET daily_pomodoro_target = ?, weekly_pomodoro_target = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dailyTarget);
            pstmt.setInt(2, weeklyTarget);
            pstmt.setLong(3, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
