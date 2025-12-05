package com.pomodone.service;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskServiceTest {

    private FakeTaskRepository repo;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        repo = new FakeTaskRepository();
        taskService = new TaskService(repo);
    }

    @AfterEach
    void tearDown() {
        repo = null;
        taskService = null;
    }

    @Test
    void createNewTask_suksesDenganDefault() {
        // bikin task baru, difficulty null harus jadi SEDANG dan status default belum selesai
        taskService.createNewTask("Belajar", "desc", null, null);

        Task saved = repo.lastSaved;
        assertEquals("Belajar", saved.getTitle());
        assertEquals("desc", saved.getDescription());
        assertEquals(TaskDifficulty.SEDANG, saved.getDifficulty()); // default kalo null
        assertEquals(TaskStatus.BELUM_SELESAI, saved.getStatus());
    }

    @Test
    void createNewTask_judulKosongLemparError() {
        assertThrows(IllegalArgumentException.class, () -> taskService.createNewTask("  ", "desc", null, TaskDifficulty.MUDAH));
        assertEquals(0, repo.saveCalls);
    }

    @Test
    void updateTask_mergeFieldBaruKeLama() {
        // simulasi data lama
        LocalDateTime oldDue = LocalDateTime.now().plusDays(1);
        Task lama = Task.builder()
                .id(7)
                .title("Lama")
                .description("Old")
                .dueDate(oldDue)
                .difficulty(TaskDifficulty.MUDAH)
                .status(TaskStatus.BELUM_SELESAI)
                .build();
        repo.store.put(7L, lama);

        LocalDateTime newDue = oldDue.plusDays(2);
        // update dengan field baru
        taskService.updateTask(
                7L,
                "Baru",
                "New Desc",
                newDue,
                TaskDifficulty.SULIT,
                TaskStatus.SELESAI
        );

        Task updated = repo.lastUpdated;
        assertEquals(7L, updated.getId());
        assertEquals("Baru", updated.getTitle());
        assertEquals("New Desc", updated.getDescription());
        assertEquals(newDue, updated.getDueDate());
        assertEquals(TaskDifficulty.SULIT, updated.getDifficulty());
        assertEquals(TaskStatus.SELESAI, updated.getStatus());
    }

    @Test
    void updateTask_idTidakAdaLemparError() {
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(99L, "x", "y", null, TaskDifficulty.SEDANG, TaskStatus.BELUM_SELESAI));
        assertEquals(0, repo.updateCalls);
    }

    @Test
    void deleteTask_idTidakValidLemparError() {
        assertThrows(IllegalArgumentException.class, () -> taskService.deleteTask(0));
        assertEquals(0, repo.deleteCalls);
    }

    @Test
    void deleteTask_idValidMenghapus() {
        Task t = Task.builder()
                .id(5)
                .title("hapus")
                .description("d")
                .difficulty(TaskDifficulty.SEDANG)
                .status(TaskStatus.BELUM_SELESAI)
                .build();
        repo.store.put(5L, t);

        taskService.deleteTask(5);

        assertEquals(1, repo.deleteCalls);
        assertFalse(repo.store.containsKey(5L));
        // di fake repo, delete tidak otomatis remove key yg tidak ada, tapi di sini sudah dipanggil
    }

    @Test
    void createNewTask_dueDateMundurStatusTerlambat() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        taskService.createNewTask("Terlambat", "desc", yesterday, TaskDifficulty.MUDAH);

        assertEquals(TaskStatus.TERLAMBAT, repo.lastSaved.getStatus());
        assertEquals(TaskDifficulty.MUDAH, repo.lastSaved.getDifficulty());
    }

    @Test
    void updateTask_fieldNullPakaiNilaiLama() {
        Task lama = Task.builder()
                .id(10)
                .title("Asli")
                .description("Desc lama")
                .dueDate(LocalDateTime.now().plusDays(3))
                .difficulty(TaskDifficulty.SEDANG)
                .status(TaskStatus.BELUM_SELESAI)
                .build();
        repo.store.put(10L, lama);

        // pakai judul/difficulty lama, kosongin desc/due supaya fallback
        taskService.updateTask(10L, "Asli", null, null, TaskDifficulty.SEDANG, null);

        Task updated = repo.lastUpdated;
        assertEquals("Asli", updated.getTitle());
        assertEquals("Desc lama", updated.getDescription());
        assertEquals(null, updated.getDueDate()); // updateTask akan menimpa dueDate dengan null jika dikirim null
        assertEquals(TaskDifficulty.SEDANG, updated.getDifficulty());
        assertEquals(TaskStatus.BELUM_SELESAI, updated.getStatus());
    }

    // Fake repo biar ga sentuh DB, dan keliatan berapa kali dipanggil
    private static class FakeTaskRepository extends TaskRepository {
        final Map<Long, Task> store = new HashMap<>();
        Task lastSaved;
        Task lastUpdated;
        long saveCalls;
        long updateCalls;
        long deleteCalls;

        @Override
        public void save(Task task) {
            saveCalls++;
            long id = task.getId() != 0 ? task.getId() : saveCalls;
            store.put(id, task);
            lastSaved = task;
        }

        @Override
        public Task findById(long id) {
            return store.get(id);
        }

        @Override
        public void update(Task updatedTask) {
            updateCalls++;
            store.put(updatedTask.getId(), updatedTask);
            lastUpdated = updatedTask;
        }

        @Override
        public void delete(int id) {
            deleteCalls++;
            store.remove((long) id);
        }
    }
}
