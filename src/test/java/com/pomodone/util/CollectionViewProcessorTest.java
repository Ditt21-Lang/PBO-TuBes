package com.pomodone.util;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionViewProcessorTest {

    private final CollectionViewProcessor<TestItem> processor = new CollectionViewProcessor<>();

    @Test
    void apply_filtersAndSortsAscending() {
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        AtomicInteger filterHits = new AtomicInteger();
        Predicate<TestItem> predicate = item -> {
            filterHits.incrementAndGet();
            return item != b; // buang B
        };

        Comparator<TestItem> comparator = Comparator.comparingInt(TestItem::rank);

        List<TestItem> result = processor.apply(source, predicate, comparator, SortDirection.ASC);

        assertEquals(List.of(a, c), result);
        assertEquals(3, filterHits.get());
    }

    @Test
    void apply_respectsDescendingDirection() {
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        AtomicInteger filterHits = new AtomicInteger();
        Predicate<TestItem> predicate = item -> {
            filterHits.incrementAndGet();
            return true;
        };

        Comparator<TestItem> comparator = Comparator.comparingInt(TestItem::rank);

        List<TestItem> result = processor.apply(source, predicate, comparator, SortDirection.DESC);

        assertEquals(List.of(c, a, b), result);
        assertEquals(3, filterHits.get());
    }

    @Test
    void apply_tanpaFilter() {
        // filter null berarti semua lolos
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        List<TestItem> source = List.of(a, b);

        Comparator<TestItem> comparator = Comparator.comparingInt(TestItem::rank);

        List<TestItem> result = processor.apply(source, null, comparator, SortDirection.ASC);

        assertEquals(List.of(b, a), result);
    }

    @Test
    void apply_tanpaSorter() {
        // comparator null berarti urutan asli dipertahankan setelah filter
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        AtomicInteger filterHits = new AtomicInteger();
        Predicate<TestItem> predicate = item -> {
            filterHits.incrementAndGet();
            return item != b;
        };

        List<TestItem> result = processor.apply(source, predicate, null, SortDirection.DESC);

        assertEquals(List.of(a, c), result);
        assertEquals(3, filterHits.get());
    }

    private record TestItem(String name, int rank) {}
}
