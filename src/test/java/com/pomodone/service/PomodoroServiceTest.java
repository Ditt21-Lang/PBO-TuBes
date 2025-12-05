package com.pomodone.service;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PomodoroServiceTest {

    private FakePomodoroSessionService sessionService;
    private PomodoroService pomodoroService;

    // Inisialisasi JavaFX sekali buat semua test
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX Platform failed to start");
            }
        } catch (IllegalStateException e) {
            // JavaFX sudah berjalan
        }
    }

    @BeforeEach
    void setUp() {
        // Bikin fake service manual
        sessionService = new FakePomodoroSessionService();

        // Setup service di thread JavaFX
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService = PomodoroService.getInstance();
            pomodoroService.setSessionService(sessionService);
            pomodoroService.stopAndResetTimer(); // reset state
            latch.countDown();
        });
        
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void handleStartPause_ShouldToggleState() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // mulai dari STOPPED
            assertEquals(PomodoroService.TimerState.STOPPED, pomodoroService.timerStateProperty().get());

            // mulai lalu pause
            pomodoroService.handleStartPause();
            assertEquals(PomodoroService.TimerState.RUNNING, pomodoroService.timerStateProperty().get());

            pomodoroService.handleStartPause();
            assertEquals(PomodoroService.TimerState.PAUSED, pomodoroService.timerStateProperty().get());
            
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void stopAndResetTimer_ShouldResetState() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.handleStartPause(); // mulai
            pomodoroService.stopAndResetTimer();

            assertEquals(PomodoroService.TimerState.STOPPED, pomodoroService.timerStateProperty().get());
            assertEquals(0.0, pomodoroService.progressProperty().get());
            
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void selectMode_ShouldUpdateSettings() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.selectMode(PomodoroService.PomodoroMode.INTENSE);
            assertEquals(PomodoroService.PomodoroMode.INTENSE, pomodoroService.getCurrentMode());
            
            pomodoroService.selectMode(PomodoroService.PomodoroMode.CLASSIC);
            assertEquals(PomodoroService.PomodoroMode.CLASSIC, pomodoroService.getCurrentMode());
            
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void focusKeShortBreakSaatWaktuHabis() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.stopAndResetTimer();
            pomodoroService.handleStartPause(); // mulai fokus
            setField("alarmSound", null); // biar callback alarm langsung jalan
            setField("timeRemaining", Duration.ofSeconds(1));
            setField("currentSessionTotalDuration", Duration.ofSeconds(1));
            invokeTick();

            assertEquals(PomodoroService.SessionType.SHORT_BREAK, getSessionType());
            assertEquals(PomodoroService.TimerState.RUNNING, pomodoroService.timerStateProperty().get());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void focusKeLongBreakSaatRondeKeempat() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.stopAndResetTimer();
            pomodoroService.handleStartPause(); // mulai fokus
            setField("alarmSound", null); // biar callback alarm langsung jalan
            setField("roundsCompleted", 3); // seolah sudah 3 focus selesai
            setField("timeRemaining", Duration.ofSeconds(1));
            setField("currentSessionTotalDuration", Duration.ofSeconds(1));
            invokeTick();

            assertEquals(PomodoroService.SessionType.LONG_BREAK, getSessionType());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void applyCustomSettings_90MenitTampilkanJam() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.applyCustomSettings(90, 5, 15, 2);
            assertTrue(pomodoroService.showHoursProperty().get());
            assertEquals("01", pomodoroService.hoursProperty().get());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void applyCustomSettings_invalidTidakUbahMode() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService.selectMode(PomodoroService.PomodoroMode.CLASSIC);
            pomodoroService.applyCustomSettings(0, 5, 15, 2); // invalid
            assertEquals(PomodoroService.PomodoroMode.CLASSIC, pomodoroService.getCurrentMode());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // Class ini berpura-pura menjadi PomodoroSessionService
    private static class FakePomodoroSessionService extends PomodoroSessionService {
        
    }

    private void setField(String name, Object value) {
        try {
            Field f = PomodoroService.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(pomodoroService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeTick() {
        try {
            Method m = PomodoroService.class.getDeclaredMethod("tick");
            m.setAccessible(true);
            m.invoke(pomodoroService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PomodoroService.SessionType getSessionType() {
        try {
            Field f = PomodoroService.class.getDeclaredField("sessionType");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            ReadOnlyObjectWrapper<PomodoroService.SessionType> wrapper =
                    (ReadOnlyObjectWrapper<PomodoroService.SessionType>) f.get(pomodoroService);
            return wrapper.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
