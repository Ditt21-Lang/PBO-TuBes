package com.pomodone.strategy.task;

import com.pomodone.model.task.Task;
import com.pomodone.util.SortDirection;

import java.time.LocalDateTime;
import java.util.Comparator;

public class DueDateAscSortStrategy implements TaskSortStrategy {
    private final Comparator<Task> comparator = Comparator.comparing(
            Task::getDueDate,
            Comparator.nullsLast(LocalDateTime::compareTo)
    );

    @Override
    public Comparator<Task> getComparator() {
        return comparator;
    }

    @Override
    public SortDirection getDirection() {
        return SortDirection.ASC;
    }
}
