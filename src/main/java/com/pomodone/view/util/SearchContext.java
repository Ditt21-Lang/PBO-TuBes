package com.pomodone.view.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SearchContext {
    private static final AtomicReference<String> pendingQuery = new AtomicReference<>();
    private static final AtomicBoolean pendingAdd = new AtomicBoolean(false);

    private SearchContext() {}

    public static void setPendingQuery(String query) {
        pendingQuery.set(query);
    }

    public static String consumePendingQuery() {
        return pendingQuery.getAndSet(null);
    }

    public static void setPendingAdd(boolean value) {
        pendingAdd.set(value);
    }

    public static boolean consumePendingAdd() {
        return pendingAdd.getAndSet(false);
    }
}
