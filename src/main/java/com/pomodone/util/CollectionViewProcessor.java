package com.pomodone.util;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CollectionViewProcessor<T> {
    public List<T> apply(List<T> source,
                         Predicate<T> filter,
                         Comparator<T> comparator,
                         SortDirection direction) {
        Objects.requireNonNull(source, "source list tidak boleh null");

        Stream<T> stream = source.stream();
        if (filter != null) {
            stream = stream.filter(filter);
        }

        if (comparator != null) {
            Comparator<T> effectiveComparator = direction == SortDirection.DESC
                    ? comparator.reversed()
                    : comparator;
            stream = stream.sorted(effectiveComparator);
        }

        return stream.toList();
    }
}
