package com.pomodone.view.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchContextTest {

    @BeforeEach
    void setUp() {
        SearchContext.setPendingQuery(null);
    }

    @AfterEach
    void tearDown() {
        SearchContext.setPendingQuery(null);
    }

    @Test
    void setAndConsume() {
        SearchContext.setPendingQuery("halo");
        assertEquals("halo", SearchContext.consumePendingQuery());
        assertNull(SearchContext.consumePendingQuery()); // sudah dikosongkan
    }
}
