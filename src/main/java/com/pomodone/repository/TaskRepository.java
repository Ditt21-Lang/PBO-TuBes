package com.pomodone.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;

import java.sql.*;
import java.util.ArrayList; 
import java.util.List;
import java.util.Optional;

public class TaskRepository {

    public Task findById(long id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Task.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("judul_tugas"))
                            .description(rs.getString("deskripsi_tugas"))
                            .dueDate(rs.getTimestamp("tenggat_tugas").toLocalDateTime())
                            .difficulty(TaskDifficulty.valueOf(rs.getString("tingkat_kesulitan")))
                            .status(TaskStatus.valueOf(rs.getString("status")))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .build();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    
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
    
    public void delete (int id){
        String query = "DELETE FROM tasks WHERE id = ?";

         try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();

            System.out.println("Task dengan nama Berhasil Dihapus");
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void update(Task updatedTask) {

        // 1. Ambil data lama dari database
        Task oldTask = findById(updatedTask.getId());
        if (oldTask == null) {
            throw new IllegalArgumentException("Task dengan ID " + updatedTask.getId() + " tidak ditemukan");
        }

        // 2. Gabungkan field lama + baru
        Task mergedTask = oldTask.withUpdatedFields(updatedTask);

        
        String sql = """
            UPDATE tasks SET 
                judul_tugas = ?, 
                deskripsi_tugas = ?, 
                tenggat_tugas = ?, 
                tingkat_kesulitan = ?::task_difficulty, 
                status = ?::task_status, 
                updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, mergedTask.getTitle());
            stmt.setString(2, mergedTask.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(mergedTask.getDueDate()));
            stmt.setString(4, mergedTask.getDifficulty().name());
            stmt.setString(5, mergedTask.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(mergedTask.getUpdatedAt()));
            stmt.setLong(7, mergedTask.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
