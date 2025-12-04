package com.pomodone.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.pomodone.config.DatabaseConfig;
import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList; 
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static final Logger log = LoggerFactory.getLogger(TaskRepository.class);

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "judul_tugas";
    private static final String COLUMN_DESCRIPTION = "deskripsi_tugas";
    private static final String COLUMN_DUE_DATE = "tenggat_tugas";
    private static final String COLUMN_DIFFICULTY = "tingkat_kesulitan";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String SELECT_ALL = "SELECT * FROM ";

    public Task findById(long id) {
        String sql = SELECT_ALL +"tasks WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp due = rs.getTimestamp(COLUMN_DUE_DATE);
                    return Task.builder()
                            .id(rs.getLong(COLUMN_ID))
                            .title(rs.getString(COLUMN_TITLE))
                            .description(rs.getString(COLUMN_DESCRIPTION))
                            .dueDate(due != null ? due.toLocalDateTime() : null)
                            .difficulty(TaskDifficulty.valueOf(rs.getString(COLUMN_DIFFICULTY)))
                            .status(TaskStatus.valueOf(rs.getString(COLUMN_STATUS)))
                            .createdAt(rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime())
                            .updatedAt(rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime())
                            .build();
                }
            }

        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal mencari task dengan id " + id, e);
        }

        return null;
    }

    
    public void save(Task task) {
        String sql = "INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status, created_at, updated_at)" +
                     "VALUES(?, ?, ?, ?, ?, ?, ?)";
    
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
            throw new com.pomodone.exception.DatabaseException("Gagal menyimpan tugas ke dalam Database", e);
        }
    } 


    public Optional<Task> findByTitle(String title) {
        String sql = SELECT_ALL + "tasks WHERE judul_tugas = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Task found = Task.builder()
                                .id(rs.getLong(COLUMN_ID))
                                .title(rs.getString(COLUMN_TITLE))
                                .description(rs.getString(COLUMN_DESCRIPTION))
                                .dueDate(rs.getTimestamp(COLUMN_DUE_DATE) != null ? rs.getTimestamp(COLUMN_DUE_DATE).toLocalDateTime() : null)
                                .difficulty(TaskDifficulty.valueOf(rs.getString(COLUMN_DIFFICULTY)))
                                .status(TaskStatus.valueOf(rs.getString(COLUMN_STATUS)))
                                .createdAt(rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime())
                                .updatedAt(rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime())
                                .build();

                    return Optional.of(found);
                }
            }


        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal membuka detail tugas ke dalam Database", e);
        } 

        return Optional.empty();
    }


    public List<Task> findAll() {
        String sql = SELECT_ALL + """
            tasks
            ORDER BY CASE 
            WHEN tenggat_tugas IS NULL 
            THEN 1 ELSE 0 END, tenggat_tugas ASC, created_at DESC
        """;
        List<Task> taskList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Task task = Task.builder()
                            .id(rs.getLong(COLUMN_ID))
                            .title(rs.getString(COLUMN_TITLE))
                            .description(rs.getString(COLUMN_DESCRIPTION))
                            .dueDate(rs.getTimestamp(COLUMN_DUE_DATE) != null ? rs.getTimestamp(COLUMN_DUE_DATE).toLocalDateTime() : null)
                            .difficulty(TaskDifficulty.valueOf(rs.getString(COLUMN_DIFFICULTY)))
                            .status(TaskStatus.valueOf(rs.getString(COLUMN_STATUS)))
                            .createdAt(rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime())
                            .updatedAt(rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime())
                            .build();

                taskList.add(task);
            }
        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal memuat database.", e);
        }

        return taskList;
    }
    
    public void delete (int id){
        String query = "DELETE FROM tasks WHERE id = ?";

         try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();

            log.info("Task {} berhasil dihapus", id);
        } catch (Exception e){
            throw new com.pomodone.exception.DatabaseException("Gagal menghapus task " + id, e);
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
                tingkat_kesulitan = ?, 
                status = ?, 
                updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, mergedTask.getTitle());
            stmt.setString(2, mergedTask.getDescription());
            Timestamp dueTimestamp = mergedTask.getDueDate() != null ? Timestamp.valueOf(mergedTask.getDueDate()) : null;
            stmt.setTimestamp(3, dueTimestamp);
            stmt.setString(4, mergedTask.getDifficulty().name());
            stmt.setString(5, mergedTask.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(mergedTask.getUpdatedAt()));
            stmt.setLong(7, mergedTask.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal update task " + updatedTask.getId(), e);
        }
    }

    public int countActiveTasks() {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status <> 'SELESAI'";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e)  {
            throw new com.pomodone.exception.DatabaseException("Gagal menghitung task aktif", e);
        }
        return 0;
    }

    public int countCompletedTasks() {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = 'SELESAI'";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal menghitung task selesai", e);
        }
        return 0;
    }

    public int countCompletedOnTimeTasks() {
        String sql = """
            SELECT COUNT(*) FROM tasks
            WHERE status = 'SELESAI'
              AND (tenggat_tugas IS NULL OR updated_at <= tenggat_tugas)
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new com.pomodone.exception.DatabaseException("Gagal menghitung task selesai tepat waktu", e);
        }
        return 0;
    }

    public List<Task> findTopByDueDate(int limit) {
        String sql = SELECT_ALL + 
        "tasks WHERE status <> 'SELESAI' ORDER BY CASE WHEN tenggat_tugas IS NULL THEN 1 ELSE 0 END, tenggat_tugas ASC, created_at DESC LIMIT ? ";
        List<Task> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp due = rs.getTimestamp(COLUMN_DUE_DATE);
                    Task task = Task.builder()
                            .id(rs.getLong(COLUMN_ID))
                            .title(rs.getString(COLUMN_TITLE))
                            .description(rs.getString(COLUMN_DESCRIPTION))
                            .dueDate(due != null ? due.toLocalDateTime() : null)
                            .difficulty(TaskDifficulty.valueOf(rs.getString(COLUMN_DIFFICULTY)))
                            .status(TaskStatus.valueOf(rs.getString(COLUMN_STATUS)))
                            .createdAt(rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime())
                            .updatedAt(rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime())
                            .build();
                    result.add(task);
                }
            }
        } catch (SQLException e) {
            log.error("Gagal mengambil task teratas berdasarkan tenggat", e);
        }
        return result;
    }

}
