package com.pomodone.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PomodoroServiceTest {

    private FakePomodoroSessionService sessionService;
    private PomodoroService pomodoroService;

    // Inisialisasi JavaFX Platform sekali sebelum semua test dijalankan
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
        // 1. Buat Fake Object manual
        sessionService = new FakePomodoroSessionService();

        // 2. Setup Service di dalam Thread JavaFX
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pomodoroService = PomodoroService.getInstance();
            pomodoroService.setSessionService(sessionService);
            pomodoroService.stopAndResetTimer(); // Reset state
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
            // Initial state should be STOPPED
            assertEquals(PomodoroService.TimerState.STOPPED, pomodoroService.timerStateProperty().get());

            // Start
            pomodoroService.handleStartPause();
            assertEquals(PomodoroService.TimerState.RUNNING, pomodoroService.timerStateProperty().get());

            // Pause
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
            pomodoroService.handleStartPause(); // Start
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

    // Class ini berpura-pura menjadi PomodoroSessionService
    private static class FakePomodoroSessionService extends PomodoroSessionService {
        
    }
}