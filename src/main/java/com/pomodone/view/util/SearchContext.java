package com.pomodone.view.util;

import java.util.concurrent.atomic.AtomicReference;

public class SearchContext {
    private static final AtomicReference<String> pendingQuery = new AtomicReference<>();

    private SearchContext() {}

    public static void setPendingQuery(String query) {
        pendingQuery.set(query);
    }

    public static String consumePendingQuery() {
        return pendingQuery.getAndSet(null);
    }
}
