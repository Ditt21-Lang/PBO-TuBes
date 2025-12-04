package com.pomodone.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

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
        String url = firstNonEmpty(env.get("DB_URL"), "jdbc:sqlite:./pomodone.db");
        String user = firstNonEmpty(env.get("DB_USER"), "postgres");
        String password = firstNonEmpty(env.get("DB_PASSWORD"), "");

        DataSource primary = null;
        DataSource resolved;
        try {
            primary = buildDataSource(url, user, password);
            runMigrations(primary);
            resolved = primary;
        } catch (Exception e) {
            if (primary instanceof HikariDataSource) {
                ((HikariDataSource) primary).close();
            }
            if (isSqlite(url)) {
                throw new RuntimeException("Gagal inisialisasi SQLite: " + e.getMessage(), e);
            }
            // Koneksi utama gagal (URL salah/psql down), pakai SQLite sebagai fallback lokal
            System.err.println("Koneksi DB utama gagal, fallback ke SQLite. Penyebab: " + e.getMessage());
            String fallbackUrl = "jdbc:sqlite:./pomodone.db";
            DataSource fallback = buildDataSource(fallbackUrl, null, null);
            runMigrations(fallback);
            resolved = fallback;
        }
        this.dataSource = resolved;
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

    private void runMigrations(DataSource ds) {
        try {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(true)
                    .locations("classpath:db/flyway")
                    .load()
                    .migrate();
        } catch (Exception e) {
            throw new RuntimeException("Gagal menjalankan migrasi database: " + e.getMessage(), e);
        }
    }

    private boolean isSqlite(String url) {
        return url != null && url.startsWith("jdbc:sqlite");
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private DataSource buildDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        boolean sqlite = isSqlite(url);
        if (sqlite) {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("PomodoneSqlitePool");
        } else {
            config.setDriverClassName("org.postgresql.Driver");
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            config.setPoolName("PomodonePgPool");
        }
        return new HikariDataSource(config);
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
