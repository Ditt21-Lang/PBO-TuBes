package com.pomodone.service;

import com.pomodone.repository.PomodoroSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PomodoroSessionServiceTest {

    private PomodoroSessionService service;
    private FakeRepo fakeRepo;

    @BeforeEach
    void setUp() throws Exception {
        service = new PomodoroSessionService();
        fakeRepo = new FakeRepo();
        inject(service, "repository", fakeRepo);
    }

    @AfterEach
    void tearDown() {
        service = null;
        fakeRepo = null;
    }

    @Test
    void logCompletedSession_menyimpanJikaValid() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(25);
        LocalDateTime end = LocalDateTime.now();

        service.logCompletedSession(start, end, 1500, PomodoroService.PomodoroMode.CLASSIC);

        assertEquals(1, fakeRepo.insertCalls);
    }

    @Test
    void logCompletedSession_abaikanJikaStartNull() {
        service.logCompletedSession(null, LocalDateTime.now(), 100, PomodoroService.PomodoroMode.CUSTOM);
        assertEquals(0, fakeRepo.insertCalls);
    }

    @Test
    void countToday() {
        fakeRepo.countReturn = 3;
        assertEquals(3, service.getTodayCompletedSessions());
    }

    @Test
    void countWeek() {
        fakeRepo.countReturn = 7;
        assertEquals(7, service.getCurrentWeekCompletedSessions());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = PomodoroSessionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeRepo extends PomodoroSessionRepository {
        int insertCalls;
        int countReturn;

        @Override
        public void insertCompletedSession(long userId, LocalDateTime startedAt, LocalDateTime endedAt, long durationSeconds, PomodoroService.PomodoroMode mode) {
            insertCalls++;
        }

        @Override
        public int countSessionsSince(long userId, LocalDateTime startInclusive) {
            return countReturn;
        }
    }
}
