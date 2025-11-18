package com.pomodone.app;

import com.pomodone.config.DatabaseConfig;

public class MainApp {
    public static void main(String[] args) {
        boolean connected = DatabaseConfig.getInstance().testConnection();
        System.out.println("DB connected? " + connected);
    }
}
