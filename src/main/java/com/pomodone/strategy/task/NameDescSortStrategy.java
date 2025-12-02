package com.pomodone.strategy.task;

import com.pomodone.model.task.Task;
import com.pomodone.util.SortDirection;

import java.util.Comparator;

public class NameDescSortStrategy implements TaskSortStrategy {
    private final Comparator<Task> comparator = Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);

    @Override
    public Comparator<Task> getComparator() {
        return comparator;
    }

    @Override
    public SortDirection getDirection() {
        return SortDirection.DESC;
    }
}
