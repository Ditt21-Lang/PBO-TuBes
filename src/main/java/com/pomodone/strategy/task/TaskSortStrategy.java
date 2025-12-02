package com.pomodone.strategy.task;

import com.pomodone.model.task.Task;
import com.pomodone.util.SortDirection;

import java.util.Comparator;

public interface TaskSortStrategy {
    Comparator<Task> getComparator();
    SortDirection getDirection();
}
