package com.pomodone.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private final DataSource dataSource;

    private DatabaseConfig() {
        Map<String, String> env = loadEnv();
        String defaultSqliteUrl = "jdbc:sqlite:" + defaultDataDir().resolve("pomodone.db");
        String url = firstNonEmpty(env.get("DB_URL"), defaultSqliteUrl);
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
            log.warn("Koneksi DB utama gagal, fallback ke SQLite: {}", e.getMessage());
            String fallbackUrl = defaultSqliteUrl;
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

    private Path defaultDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = appData != null ? Paths.get(appData) : Paths.get(System.getProperty("user.home", "."));
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home", "."), "Library", "Application Support");
        } else {
            base = Paths.get(System.getProperty("user.home", "."), ".local", "share");
        }

        Path dataDir = base.resolve("pomodone");
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            log.warn("Gagal membuat folder data {}, fallback ke current dir", dataDir, e);
            return Paths.get(".");
        }
        return dataDir;
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
