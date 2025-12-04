package com.pomodone.strategy.task;

import com.pomodone.model.task.Task;
import com.pomodone.model.task.TaskDifficulty;
import com.pomodone.util.SortDirection;

import java.util.Comparator;

public class DifficultyAscSortStrategy implements TaskSortStrategy {
    private final Comparator<Task> comparator = Comparator.comparingInt(task -> mapDifficulty(task.getDifficulty()));

    @Override
    public Comparator<Task> getComparator() {
        return comparator;
    }

    @Override
    public SortDirection getDirection() {
        return SortDirection.ASC;
    }

    private int mapDifficulty(TaskDifficulty difficulty) {
        return switch (difficulty) {
            case MUDAH -> 1;
            case SEDANG -> 2;
            case SULIT -> 3;
        };
    }
}
