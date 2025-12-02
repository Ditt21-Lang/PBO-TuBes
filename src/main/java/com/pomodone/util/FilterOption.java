package com.pomodone.util;

import java.util.function.Predicate;

public record FilterOption<T>(String label, Predicate<T> predicate) {
    @Override
    public String toString() {
        return label;
    }
}
