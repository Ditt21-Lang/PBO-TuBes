package com.pomodone.service;

import com.pomodone.model.pomodoro.CustomPomodoroPreset;
import com.pomodone.repository.PomodoroPresetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomPomodoroPresetServiceTest {

    private CustomPomodoroPresetService service;
    private FakeRepo fakeRepo;

    @BeforeEach
    void setUp() throws Exception {
        service = new CustomPomodoroPresetService();
        fakeRepo = new FakeRepo();
        inject(service, "repository", fakeRepo);
    }

    @AfterEach
    void tearDown() {
        service = null;
        fakeRepo = null;
    }

    @Test
    void loadLatestPreset_dariRepo() {
        CustomPomodoroPreset preset = new CustomPomodoroPreset(1, 1, "Custom", 20, 3, 10, 4);
        fakeRepo.latest = preset;

        CustomPomodoroPreset loaded = service.loadLatestPreset();

        assertEquals(preset, loaded);
    }

    @Test
    void savePreset_menyimpanKeRepo() {
        service.savePreset(30, 5, 15, 6);

        assertEquals(30, fakeRepo.savedPreset.getFocusMinutes());
        assertEquals(5, fakeRepo.savedPreset.getShortBreakMinutes());
        assertEquals(15, fakeRepo.savedPreset.getLongBreakMinutes());
        assertEquals(6, fakeRepo.savedPreset.getRounds());
        assertEquals("Custom", fakeRepo.savedPreset.getPresetName());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = CustomPomodoroPresetService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeRepo extends PomodoroPresetRepository {
        CustomPomodoroPreset latest;
        CustomPomodoroPreset savedPreset;

        @Override
        public CustomPomodoroPreset findLatestByUser(long userId) {
            return latest;
        }

        @Override
        public void upsert(long userId, CustomPomodoroPreset preset) {
            this.savedPreset = preset;
        }
    }
}
