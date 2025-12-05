package com.pomodone.model.task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
    }

    @AfterEach
    void tearDown() {
        now = null;
    }

    @Test
    void builder_kalauDueLewat_statusJadiTerlambat() {
        Task t = Task.builder()
                .id(1)
                .title("Telat")
                .description("desc")
                .dueDate(now.minusDays(1))
                .difficulty(TaskDifficulty.MUDAH)
                .status(TaskStatus.BELUM_SELESAI)
                .build();

        assertEquals(TaskStatus.TERLAMBAT, t.getStatus());
    }

    @Test
    void builder_wajibTitleDanDifficulty() {
        assertThrows(NullPointerException.class, () -> Task.builder().difficulty(TaskDifficulty.MUDAH).build());
    }

    @Test
    void withUpdatedFields_mergeNullPakaiLama() {
        Task base = Task.builder()
                .id(2)
                .title("Awal")
                .description("Old")
                .dueDate(now.plusDays(1))
                .difficulty(TaskDifficulty.SEDANG)
                .status(TaskStatus.BELUM_SELESAI)
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(1))
                .build();

        Task patch = Task.builder()
                .title("Awal") // wajib isi
                .description(null) // biarin null supaya fallback ke lama
                .dueDate(null)
                .difficulty(TaskDifficulty.SEDANG)
                .status(TaskStatus.BELUM_SELESAI)
                .build();

        Task merged = base.withUpdatedFields(patch);

        assertEquals("Awal", merged.getTitle());
        assertEquals("Old", merged.getDescription());
        // dueDate jadi null karena patch null menimpa nilai lama
        assertEquals(null, merged.getDueDate());
        assertEquals(TaskDifficulty.SEDANG, merged.getDifficulty());
        assertEquals(TaskStatus.BELUM_SELESAI, merged.getStatus());
        assertNotNull(merged.getUpdatedAt());
    }
}
