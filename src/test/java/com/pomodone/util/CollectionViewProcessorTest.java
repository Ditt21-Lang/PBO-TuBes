package com.pomodone.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionViewProcessorTest {

    @Mock
    private Predicate<TestItem> predicate;

    @Mock
    private Comparator<TestItem> comparator;

    private final CollectionViewProcessor<TestItem> processor = new CollectionViewProcessor<>();

    @Test
    void apply_filtersAndSortsAscending() {
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        // Only keep A and C
        when(predicate.test(a)).thenReturn(true);
        when(predicate.test(b)).thenReturn(false);
        when(predicate.test(c)).thenReturn(true);

        // Sort by rank ascending
        Comparator<TestItem> delegate = Comparator.comparingInt(TestItem::rank);
        when(comparator.compare(any(), any()))
                .thenAnswer(inv -> delegate.compare(inv.getArgument(0), inv.getArgument(1)));

        List<TestItem> result = processor.apply(source, predicate, comparator, SortDirection.ASC);

        assertEquals(List.of(a, c), result);
        verify(predicate, times(3)).test(any());
        verify(comparator, atLeast(1)).compare(any(), any());
    }

    @Test
    void apply_respectsDescendingDirection() {
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        when(predicate.test(any())).thenReturn(true);

        Comparator<TestItem> delegate = Comparator.comparingInt(TestItem::rank);
        when(comparator.compare(any(), any()))
                .thenAnswer(inv -> delegate.compare(inv.getArgument(0), inv.getArgument(1)));

        List<TestItem> result = processor.apply(source, predicate, comparator, SortDirection.DESC);

        assertEquals(List.of(c, a, b), result);
        verify(predicate, times(3)).test(any());
        verify(comparator, atLeast(1)).compare(any(), any());
    }

    @Test
    void apply_tanpaFilter() {
        // filter null berarti semua lolos
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        List<TestItem> source = List.of(a, b);

        Comparator<TestItem> delegate = Comparator.comparingInt(TestItem::rank);
        when(comparator.compare(any(), any()))
                .thenAnswer(inv -> delegate.compare(inv.getArgument(0), inv.getArgument(1)));

        List<TestItem> result = processor.apply(source, null, comparator, SortDirection.ASC);

        assertEquals(List.of(b, a), result);
        verify(comparator, atLeast(1)).compare(any(), any());
    }

    @Test
    void apply_tanpaSorter() {
        // comparator null berarti urutan asli dipertahankan setelah filter
        TestItem a = new TestItem("A", 2);
        TestItem b = new TestItem("B", 1);
        TestItem c = new TestItem("C", 3);
        List<TestItem> source = List.of(a, b, c);

        when(predicate.test(a)).thenReturn(true);
        when(predicate.test(b)).thenReturn(false);
        when(predicate.test(c)).thenReturn(true);

        List<TestItem> result = processor.apply(source, predicate, null, SortDirection.DESC);

        assertEquals(List.of(a, c), result);
        verify(predicate, times(3)).test(any());
    }

    private record TestItem(String name, int rank) {}
}
