package com.pomodone.service;

import com.pomodone.model.user.User;
import com.pomodone.repository.UserRepository;

import java.util.Optional;

public class UserSettingsService {
    private final UserRepository userRepository;
    private static final long CURRENT_USER_ID = 1; // ID user sementara, ntar diganti

    public UserSettingsService() {
        this.userRepository = new UserRepository();
    }

    public Optional<User> getCurrentUserSettings() {
        return userRepository.findById(CURRENT_USER_ID);
    }

    public boolean updateUserName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        return userRepository.updateName(CURRENT_USER_ID, newName.trim());
    }

    public boolean updateUserTargets(int dailyTarget, int weeklyTarget) {
        if (dailyTarget < 0 || weeklyTarget < 0) {
            return false;
        }
        return userRepository.updateTargets(CURRENT_USER_ID, dailyTarget, weeklyTarget);
    }
}
