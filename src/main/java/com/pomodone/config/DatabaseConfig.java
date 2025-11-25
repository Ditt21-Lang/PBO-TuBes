package com.pomodone.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {
    private static volatile DatabaseConfig instance;
    private final DataSource dataSource;

    private DatabaseConfig() {
        Map<String, String> env = loadEnv();
        String url = firstNonEmpty(env.get("DB_URL"), "jdbc:postgresql://localhost:5432/pomodone");
        String user = firstNonEmpty(env.get("DB_USER"), "postgres");
        String password = firstNonEmpty(env.get("DB_PASSWORD"), "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setPoolName("StudyFocusPool");
        this.dataSource = new HikariDataSource(config);
    }

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>(System.getenv());
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int idx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, idx).trim();
                    String value = trimmed.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        env.put(key, value);
                    }
                }
            } catch (Exception ignored) {
                // cuekin aja, fallback ke System.getenv
            }
        }
        return env;
    }
}
