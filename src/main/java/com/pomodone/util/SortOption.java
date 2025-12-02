package com.pomodone.util;

import java.util.Comparator;

public record SortOption<T>(String label, Comparator<T> comparator) {
    @Override
    public String toString() {
        return label;
    }
}
