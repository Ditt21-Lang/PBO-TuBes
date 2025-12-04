package com.pomodone.strategy.task;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.model.task.TaskStatus;
import com.pomodone.util.SortDirection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskSortStrategiesTest {

    @Test
    void nameAsc_desc() {
        Task a = task("Alpha", TaskDifficulty.MUDAH, null);
        Task b = task("beta", TaskDifficulty.SEDANG, null);
        List<Task> list = new ArrayList<>(List.of(b, a));

        // naik nama (case-insensitive), Alpha harus duluan
        list.sort(new NameAscSortStrategy().getComparator());
        assertEquals(List.of(a, b), list);
        assertEquals(SortDirection.ASC, new NameAscSortStrategy().getDirection());

        // comparator sama, tapi arah beda di processor
        list.sort(new NameDescSortStrategy().getComparator());
        assertEquals(List.of(b, a), list); // comparator sama, arah beda di view processor
        assertEquals(SortDirection.DESC, new NameDescSortStrategy().getDirection());
    }

    @Test
    void dueDateAsc_nullDiAkhir() {
        LocalDateTime now = LocalDateTime.now();
        Task t1 = task("A", TaskDifficulty.SEDANG, now.plusDays(1));
        Task t2 = task("B", TaskDifficulty.SEDANG, now);
        Task t3 = task("C", TaskDifficulty.SEDANG, null);
        List<Task> list = new ArrayList<>(List.of(t3, t1, t2));

        // null due ditaruh terakhir saat naik
        list.sort(new DueDateAscSortStrategy().getComparator());
        assertEquals(List.of(t2, t1, t3), list);
        assertEquals(SortDirection.ASC, new DueDateAscSortStrategy().getDirection());

        // Desc pakai comparator yang sama, tapi arah dibalik di processor
        assertEquals(SortDirection.DESC, new DueDateDescSortStrategy().getDirection());
    }

    @Test
    void difficultyAsc_desc() {
        Task easy = task("E", TaskDifficulty.MUDAH, null);
        Task med = task("M", TaskDifficulty.SEDANG, null);
        Task hard = task("H", TaskDifficulty.SULIT, null);
        List<Task> list = new ArrayList<>(List.of(hard, med, easy));

        // naik: mudah dulu, lalu sedang, lalu sulit
        Comparator<Task> ascComp = new DifficultyAscSortStrategy().getComparator();
        list.sort(ascComp);
        assertEquals(List.of(easy, med, hard), list);
        assertEquals(SortDirection.ASC, new DifficultyAscSortStrategy().getDirection());
        assertEquals(SortDirection.DESC, new DifficultyDescSortStrategy().getDirection());
    }

    private Task task(String title, TaskDifficulty difficulty, LocalDateTime due) {
        return Task.builder()
                .id(0)
                .title(title)
                .description("")
                .dueDate(due)
                .difficulty(difficulty)
                .status(TaskStatus.BELUM_SELESAI)
                .build();
    }
}
