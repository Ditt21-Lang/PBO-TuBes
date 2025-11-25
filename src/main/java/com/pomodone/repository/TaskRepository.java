package com.pomodone.repository;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.task.Task;
import java.sql.*;

public class TaskRepository {
    
    public void save(Task task) {
        String sql = "INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status, created_at, updated_at)" +
                     "VALUES(?, ?, ?, ?::task_difficulty, ?::task_status, ?, ?)";
    
        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setTimestamp(3, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
            pstmt.setString(4, task.getDifficulty().name());
            pstmt.setString(5, task.getStatus().name());
            pstmt.setTimestamp(6, Timestamp.valueOf(task.getCreatedAt()));
            pstmt.setTimestamp(7, Timestamp.valueOf(task.getUpdatedAt()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal menyimpan tugas ke dalam Database:" + e.getMessage());
        }
    } 
}
