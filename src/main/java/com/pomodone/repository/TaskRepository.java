package com.pomodone.repository;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;

import java.sql.*;
import java.util.ArrayList; 
import java.util.List;
import java.util.Optional;

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


    public Optional<Task> findByTitle(String title) {
        String sql = "SELECT * FROM tasks WHERE judul_tugas = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Task found = Task.builder()
                                .id(rs.getLong("id"))
                                .title(rs.getString("judul_tugas"))
                                .description(rs.getString("deskripsi_tugas"))
                                .dueDate(rs.getTimestamp("tenggat_tugas") != null ? rs.getTimestamp("tenggat_tugas").toLocalDateTime() : null)
                                .difficulty(TaskDifficulty.valueOf(rs.getString("tingkat_kesulitan")))
                                .status(TaskStatus.valueOf(rs.getString("status")))
                                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                                .build();

                    return Optional.of(found);
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal membuka detail tugas ke dalam Database:" + e.getMessage());
        } 

        return Optional.empty();
    }


    public List<Task> findAll() {
        String sql = "SELECT * FROM tasks ORDER BY created_At DESC";
        List<Task> taskList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Task task = Task.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("judul_tugas"))
                            .description(rs.getString("deskripsi_tugas"))
                            .dueDate(rs.getTimestamp("tenggat_tugas") != null ? rs.getTimestamp("tenggat_tugas").toLocalDateTime() : null)
                            .difficulty(TaskDifficulty.valueOf(rs.getString("tingkat_kesulitan")))
                            .status(TaskStatus.valueOf(rs.getString("status")))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .build();

                taskList.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal memuat database." + e.getMessage());
        }

        return taskList;
    }
}
