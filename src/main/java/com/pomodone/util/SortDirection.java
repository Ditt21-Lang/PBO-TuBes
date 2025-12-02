package com.pomodone.util;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection orDefault(SortDirection direction, SortDirection fallback) {
        return direction != null ? direction : fallback;
    }

    @Override
    public String toString() {
        return this == ASC ? "Ascending" : "Descending";
    }
}
