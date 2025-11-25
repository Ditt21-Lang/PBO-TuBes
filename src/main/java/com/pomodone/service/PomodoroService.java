package com.pomodone.service;

import com.pomodone.model.pomodoro.PomodoroSettings;
import com.pomodone.strategy.pomodoro.ClassicPomodoroStrategy;
import com.pomodone.strategy.pomodoro.CustomPomodoroStrategy;
import com.pomodone.strategy.pomodoro.IntensePomodoroStrategy;
import com.pomodone.strategy.pomodoro.PomodoroStrategy;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

public class PomodoroService {
    private static PomodoroService instance;

    public enum TimerState { STOPPED, RUNNING, PAUSED }
    public enum SessionType { FOCUS, SHORT_BREAK, LONG_BREAK }
    public enum PomodoroMode { CLASSIC, INTENSE, CUSTOM }

    // State dari service
    private Timeline timeline;
    private java.time.Duration timeRemaining;
    private java.time.Duration currentSessionTotalDuration;
    private PomodoroStrategy strategy;
    private PomodoroSettings settings;
    private int roundsCompleted = 0;
    private Media alarmSound;

    // Property yang bisa di-observe buat UI
    private final ReadOnlyStringWrapper hours = new ReadOnlyStringWrapper("00");
    private final ReadOnlyStringWrapper minutes = new ReadOnlyStringWrapper("25");
    private final ReadOnlyStringWrapper seconds = new ReadOnlyStringWrapper("00");
    private final ReadOnlyBooleanWrapper showHours = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusString = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<TimerState> timerState = new ReadOnlyObjectWrapper<>(TimerState.STOPPED);
    private final ReadOnlyObjectWrapper<SessionType> sessionType = new ReadOnlyObjectWrapper<>(SessionType.FOCUS);
    private final ObjectProperty<PomodoroMode> pomodoroMode = new SimpleObjectProperty<>(PomodoroMode.CLASSIC);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0.0);


    private PomodoroService() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        loadAlarmSound();
        selectMode(PomodoroMode.CLASSIC); // default-nya classic
    }
    
    private void loadAlarmSound() {
        try {
            URL resource = getClass().getResource("/audio/audio.mp3");
            if (resource != null) {
                alarmSound = new Media(resource.toExternalForm());
            } else {
                System.err.println("Suara alarm ga ketemu.");
            }
        } catch (Exception e) {
            System.err.println("Gagal load suara alarm.");
            e.printStackTrace();
        }
    }

    public static synchronized PomodoroService getInstance() {
        if (instance == null) {
            instance = new PomodoroService();
        }
        return instance;
    }


    public void selectMode(PomodoroMode mode) {
        if (mode == null) return;

        switch (mode) {
            case CLASSIC:
                strategy = new ClassicPomodoroStrategy();
                break;
            case INTENSE:
                strategy = new IntensePomodoroStrategy();
                break;
            case CUSTOM:
                // strategi custom di-update terpisah
                return;
        }
        this.pomodoroMode.set(mode);
        this.settings = strategy.getSettings();
        stopAndResetTimer();
    }
    
    public void applyCustomSettings(int focus, int sBreak, int lBreak, int rounds) {
        try {
            PomodoroSettings customSettings = new PomodoroSettings(
                    java.time.Duration.ofMinutes(focus),
                    java.time.Duration.ofMinutes(sBreak),
                    java.time.Duration.ofMinutes(lBreak),
                    rounds);
            this.strategy = new CustomPomodoroStrategy(customSettings);
            this.settings = this.strategy.getSettings();
            this.pomodoroMode.set(PomodoroMode.CUSTOM);
            stopAndResetTimer();
        } catch (IllegalArgumentException e) {
            System.err.println("Error pas apply custom settings: " + e.getMessage());
        }
    }

    public void handleStartPause() {
        if (timerState.get() == TimerState.RUNNING) {
            timeline.pause();
            timerState.set(TimerState.PAUSED);
        } else {
            if (timerState.get() == TimerState.STOPPED) {
                sessionType.set(SessionType.FOCUS);
                roundsCompleted = 0;
                timeRemaining = settings.getFocusDuration();
                currentSessionTotalDuration = timeRemaining;
            }
            timeline.play();
            timerState.set(TimerState.RUNNING);
        }
        updateStatusString();
    }

    public void stopAndResetTimer() {
        timeline.stop();
        timerState.set(TimerState.STOPPED);
        sessionType.set(SessionType.FOCUS);
        roundsCompleted = 0;
        if (settings != null) {
            timeRemaining = settings.getFocusDuration();
            currentSessionTotalDuration = timeRemaining;
        }
        updateTimerLabels();
        updateStatusString();
        progress.set(0.0);
    }


    public ReadOnlyStringProperty hoursProperty() { return hours.getReadOnlyProperty(); }
    public ReadOnlyStringProperty minutesProperty() { return minutes.getReadOnlyProperty(); }
    public ReadOnlyStringProperty secondsProperty() { return seconds.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showHoursProperty() { return showHours.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusStringProperty() { return statusString.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<TimerState> timerStateProperty() { return timerState.getReadOnlyProperty(); }
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }


    private void tick() {
        timeRemaining = timeRemaining.minusSeconds(1);
        updateTimerLabels();
        updateProgress();

        if (timeRemaining.isZero() || timeRemaining.isNegative()) {
            startNextSession();
        }
    }
    
    private void updateProgress() {
        if (currentSessionTotalDuration == null || currentSessionTotalDuration.isZero()) {
            progress.set(0.0);
        } else {
            double elapsed = currentSessionTotalDuration.toSeconds() - timeRemaining.toSeconds();
            double total = currentSessionTotalDuration.toSeconds();
            progress.set(total > 0 ? elapsed / total : 0);
        }
    }
    
    private void playAlarm(Runnable onAlarmFinished) {
        if (alarmSound != null) {
            MediaPlayer mediaPlayer = new MediaPlayer(alarmSound);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.play();

            // Timeline buat berhentiin alarm setelah 9 detik, trus jalanin callback
            Timeline alarmDurationTimer = new Timeline(new KeyFrame(
                Duration.seconds(9), // mainin alarm 9 detik
                event -> {
                    mediaPlayer.stop();
                    mediaPlayer.dispose(); // bebasin resource
                    if (onAlarmFinished != null) {
                        onAlarmFinished.run();
                    }
                }
            ));
            alarmDurationTimer.setCycleCount(1);
            alarmDurationTimer.play();
        } else {
            System.err.println("Suara alarm ga ke-load. Lanjut tanpa alarm.");
            if (onAlarmFinished != null) {
                onAlarmFinished.run();
            }
        }
    }

    private void startNextSession() {
        // berhentiin timeline utama, timer bakal di 00:00
        timeline.stop();

        // mainin alarm, callback-nya bakal nyiapin dan mulai sesi berikutnya
        playAlarm(() -> {
            if (sessionType.get() == SessionType.FOCUS) {
                roundsCompleted++;
                if (roundsCompleted % settings.getRoundsBeforeLongBreak() == 0) {
                    sessionType.set(SessionType.LONG_BREAK);
                    timeRemaining = settings.getLongBreakDuration();
                } else {
                    sessionType.set(SessionType.SHORT_BREAK);
                    timeRemaining = settings.getShortBreakDuration();
                }
            } else { // kalo tadi break
                sessionType.set(SessionType.FOCUS);
                timeRemaining = settings.getFocusDuration();
            }
            currentSessionTotalDuration = timeRemaining;
            updateStatusString();
            updateTimerLabels();

            timeline.play();
            timerState.set(TimerState.RUNNING);
        });
    }

    private void updateTimerLabels() {
        if (timeRemaining == null) return;
        long totalSeconds = timeRemaining.toSeconds();

        if (totalSeconds < 0) totalSeconds = 0;

        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        
        boolean shouldShowHours = (currentSessionTotalDuration != null && currentSessionTotalDuration.toHours() > 0) || h > 0;
        showHours.set(shouldShowHours);

        hours.set(String.format("%02d", h));
        minutes.set(String.format("%02d", m));
        seconds.set(String.format("%02d", s));
    }
    
    private void updateStatusString() {
        if (settings == null) return;

        if (timerState.get() == TimerState.STOPPED) {
            statusString.set(String.format("%d rounds of %d min focus, %d min break.",
                    settings.getRoundsBeforeLongBreak(),
                    settings.getFocusDuration().toMinutes(),
                    settings.getShortBreakDuration().toMinutes()));
            return;
        }

        String text = "";
        switch (sessionType.get()) {
            case FOCUS:
                 int currentRound = (roundsCompleted % settings.getRoundsBeforeLongBreak()) + 1;
                text = "Focus " + currentRound + "/" + settings.getRoundsBeforeLongBreak();
                break;
            case SHORT_BREAK:
                text = "Short Break";
                break;
            case LONG_BREAK:
                text = "Long Break";
                break;
        }
        statusString.set(text);
    }
}