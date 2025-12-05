package com.pomodone.service;

import com.pomodone.model.dashboard.DashboardStats;
import com.pomodone.model.user.User;
import com.pomodone.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardStatsServiceTest {

    private DashboardStatsService service;
    private FakePomodoroSessionService pomodoroSessionService;
    private FakeTaskRepository taskRepository;
    private FakeUserSettingsService userSettingsService;

    @BeforeEach
    void setUp() throws Exception {
        service = new DashboardStatsService();
        pomodoroSessionService = new FakePomodoroSessionService();
        taskRepository = new FakeTaskRepository();
        userSettingsService = new FakeUserSettingsService();

        inject(service, "pomodoroSessionService", pomodoroSessionService);
        inject(service, "taskRepository", taskRepository);
        inject(service, "userSettingsService", userSettingsService);
    }

    @AfterEach
    void tearDown() {
        service = null;
        pomodoroSessionService = null;
        taskRepository = null;
        userSettingsService = null;
    }

    @Test
    void loadStats_menghitungProductivity() {
        pomodoroSessionService.today = 3;
        taskRepository.activeTasks = 5;
        taskRepository.completed = 4;
        taskRepository.completedOnTime = 2;
        userSettingsService.user = new User(1, "User", 4, 20);

        DashboardStats stats = service.loadStats();

        // productivity = rata-rata pomodoroRatio (3/4=0.75) dan onTimeRatio (2/4=0.5) => 62.5 => 63
        assertEquals(3, stats.getDailyPomodoroDone());
        assertEquals(4, stats.getDailyPomodoroTarget());
        assertEquals(5, stats.getActiveTasks());
        assertEquals(63, stats.getProductivityPercent());
    }

    @Test
    void loadStats_targetKosong_dianggap0() {
        pomodoroSessionService.today = 0;
        taskRepository.activeTasks = 1;
        taskRepository.completed = 0;
        taskRepository.completedOnTime = 0;
        userSettingsService.user = null;

        DashboardStats stats = service.loadStats();

        assertEquals(0, stats.getDailyPomodoroTarget());
        assertEquals(0, stats.getProductivityPercent());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = DashboardStatsService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakePomodoroSessionService extends PomodoroSessionService {
        int today;

        @Override
        public int getTodayCompletedSessions() {
            return today;
        }
    }

    private static class FakeTaskRepository extends TaskRepository {
        int activeTasks;
        int completed;
        int completedOnTime;

        @Override
        public int countActiveTasks() {
            return activeTasks;
        }

        @Override
        public int countCompletedTasks() {
            return completed;
        }

        @Override
        public int countCompletedOnTimeTasks() {
            return completedOnTime;
        }
    }

    private static class FakeUserSettingsService extends UserSettingsService {
        User user;

        @Override
        public Optional<User> getCurrentUserSettings() {
            return Optional.ofNullable(user);
        }
    }
}
