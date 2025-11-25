package com.pomodone.service;

import com.pomodone.model.user.User;
import com.pomodone.repository.UserRepository;

import java.util.Optional;

public class UserSettingsService {
    private final UserRepository userRepository;
    private final long currentUserId = 1; // Hardcoded user ID for now

    public UserSettingsService() {
        this.userRepository = new UserRepository();
    }

    public Optional<User> getCurrentUserSettings() {
        return userRepository.findById(currentUserId);
    }

    public boolean updateUserName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        return userRepository.updateName(currentUserId, newName.trim());
    }

    public boolean updateUserTargets(int dailyTarget, int weeklyTarget) {
        if (dailyTarget < 0 || weeklyTarget < 0) {
            return false;
        }
        return userRepository.updateTargets(currentUserId, dailyTarget, weeklyTarget);
    }
}
