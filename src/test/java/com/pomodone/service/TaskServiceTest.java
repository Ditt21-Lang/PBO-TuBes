package com.pomodone.service;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() throws Exception {
        taskService = new TaskService();
        injectRepo(taskService, taskRepository);
    }

    @Test
    void createNewTask_suksesDenganDefault() {
        // bikin task baru, difficulty null harus jadi SEDANG dan status default belum selesai
        taskService.createNewTask("Belajar", "desc", null, null);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();

        assertEquals("Belajar", saved.getTitle());
        assertEquals("desc", saved.getDescription());
        assertEquals(TaskDifficulty.SEDANG, saved.getDifficulty()); // default kalo null
        assertEquals(TaskStatus.BELUM_SELESAI, saved.getStatus());
    }

    @Test
    void createNewTask_judulKosongLemparError() {
        assertThrows(IllegalArgumentException.class, () -> taskService.createNewTask("  ", "desc", null, TaskDifficulty.MUDAH));
        verify(taskRepository, never()).save(any());
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
        when(taskRepository.findById(7L)).thenReturn(lama);

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

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).update(captor.capture());
        Task updated = captor.getValue();
        assertEquals(7L, updated.getId());
        assertEquals("Baru", updated.getTitle());
        assertEquals("New Desc", updated.getDescription());
        assertEquals(newDue, updated.getDueDate());
        assertEquals(TaskDifficulty.SULIT, updated.getDifficulty());
        assertEquals(TaskStatus.SELESAI, updated.getStatus());
    }

    @Test
    void updateTask_idTidakAdaLemparError() {
        when(taskRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(99L, "x", "y", null, TaskDifficulty.SEDANG, TaskStatus.BELUM_SELESAI));
        verify(taskRepository, never()).update(any());
    }

    @Test
    void deleteTask_idTidakValidLemparError() {
        assertThrows(IllegalArgumentException.class, () -> taskService.deleteTask(0));
        verify(taskRepository, never()).delete(anyInt());
    }

    private void injectRepo(TaskService service, TaskRepository mock) throws Exception {
        Field field = TaskService.class.getDeclaredField("taskRepository");
        field.setAccessible(true);
        field.set(service, mock);
    }
}
