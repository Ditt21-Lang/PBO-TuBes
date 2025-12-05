package db.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SuppressWarnings("java:S101")
public class V1__Baseline extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String url = connection.getMetaData().getURL();
        boolean sqlite = url != null && url.startsWith("jdbc:sqlite");
        if (sqlite) {
            // Skema SQLite: pakai CHECK sebagai pengganti ENUM/trigger
            migrateSqlite(connection);
        } else {
            // Skema PostgreSQL sederhana (tanpa PLpgSQL) supaya tetap kompatibel fallback
            migratePostgres(connection);
        }
    }

    private void migrateSqlite(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    daily_pomodoro_target INTEGER NOT NULL DEFAULT 5,
                    weekly_pomodoro_target INTEGER NOT NULL DEFAULT 25,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    judul_tugas TEXT NOT NULL,
                    deskripsi_tugas TEXT,
                    tenggat_tugas DATETIME,
                    tingkat_kesulitan TEXT NOT NULL CHECK (tingkat_kesulitan IN ('SULIT','SEDANG','MUDAH')),
                    status TEXT NOT NULL DEFAULT 'BELUM_SELESAI' CHECK (status IN ('BELUM_SELESAI','TERLAMBAT','SELESAI')),
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pomodoro_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    ended_at DATETIME NOT NULL,
                    duration_seconds INTEGER NOT NULL DEFAULT 0,
                    mode TEXT NOT NULL CHECK (mode IN ('CLASSIC','INTENSE','CUSTOM')),
                    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED','CANCELLED')),
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pomo_sessions_user_started ON pomodoro_sessions (user_id, started_at DESC)");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pomodoro_custom_presets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    preset_name TEXT NOT NULL DEFAULT 'Custom',
                    focus_minutes INTEGER NOT NULL,
                    short_break_minutes INTEGER NOT NULL,
                    long_break_minutes INTEGER NOT NULL,
                    rounds INTEGER NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (user_id, preset_name)
                )
            """);

            seedUsersSqlite(stmt);
            seedTasksSqlite(stmt);
        }
    }

    private void migratePostgres(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    daily_pomodoro_target INT NOT NULL DEFAULT 5,
                    weekly_pomodoro_target INT NOT NULL DEFAULT 25,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id BIGSERIAL PRIMARY KEY,
                    judul_tugas VARCHAR(255) NOT NULL,
                    deskripsi_tugas TEXT,
                    tenggat_tugas TIMESTAMP,
                    tingkat_kesulitan VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'BELUM_SELESAI',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT task_status_check CHECK (status IN ('BELUM_SELESAI','TERLAMBAT','SELESAI')),
                    CONSTRAINT task_difficulty_check CHECK (tingkat_kesulitan IN ('SULIT','SEDANG','MUDAH'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pomodoro_sessions (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    ended_at TIMESTAMP NOT NULL,
                    duration_seconds INTEGER NOT NULL DEFAULT 0,
                    mode VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT pomo_mode_check CHECK (mode IN ('CLASSIC','INTENSE','CUSTOM')),
                    CONSTRAINT pomo_status_check CHECK (status IN ('COMPLETED','CANCELLED'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pomo_sessions_user_started ON pomodoro_sessions (user_id, started_at DESC)");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pomodoro_custom_presets (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    preset_name VARCHAR(100) NOT NULL DEFAULT 'Custom',
                    focus_minutes INTEGER NOT NULL,
                    short_break_minutes INTEGER NOT NULL,
                    long_break_minutes INTEGER NOT NULL,
                    rounds INTEGER NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (user_id, preset_name)
                )
            """);

            seedUsersPostgres(stmt);
            seedTasksPostgres(stmt);
        }
    }

    private void seedUsersSqlite(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT OR IGNORE INTO users (name, daily_pomodoro_target, weekly_pomodoro_target)
            VALUES ('Pomodone User', 5, 25), ('Guest User', 5, 25)
        """);
    }

    private void seedTasksSqlite(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Belajar Polimorfisme',
                   'Siapkan contoh kode polimorfisme untuk presentasi OOP.',
                   datetime('now', '+1 day'),
                   'SEDANG',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks)
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'UI Mockup Pomodoro',
                   'Desain tata letak tampilan utama aplikasi pomodoro.',
                   datetime('now', '+3 days'),
                   'MUDAH',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'UI Mockup Pomodoro')
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Refactor Timer Engine',
                   'Perbaiki arsitektur engine timer agar lebih modular.',
                   datetime('now', '-6 hours'),
                   'SULIT',
                   'TERLAMBAT'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'Refactor Timer Engine')
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Publikasi Release Alpha',
                   'Rilis versi alpha ke internal tester setelah regression test.',
                   NULL,
                   'SEDANG',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'Publikasi Release Alpha')
        """);
    }

    private void seedUsersPostgres(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT INTO users (name, daily_pomodoro_target, weekly_pomodoro_target)
            VALUES ('Pomodone User', 5, 25), ('Guest User', 5, 25)
            ON CONFLICT (name) DO NOTHING
        """);
    }

    private void seedTasksPostgres(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Belajar Polimorfisme',
                   'Siapkan contoh kode polimorfisme untuk presentasi OOP.',
                   CURRENT_TIMESTAMP + INTERVAL '1 day',
                   'SEDANG',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks)
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'UI Mockup Pomodoro',
                   'Desain tata letak tampilan utama aplikasi pomodoro.',
                   CURRENT_TIMESTAMP + INTERVAL '3 days',
                   'MUDAH',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'UI Mockup Pomodoro')
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Refactor Timer Engine',
                   'Perbaiki arsitektur engine timer agar lebih modular.',
                   CURRENT_TIMESTAMP - INTERVAL '6 hours',
                   'SULIT',
                   'TERLAMBAT'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'Refactor Timer Engine')
        """);
        stmt.executeUpdate("""
            INSERT INTO tasks (judul_tugas, deskripsi_tugas, tenggat_tugas, tingkat_kesulitan, status)
            SELECT 'Publikasi Release Alpha',
                   'Rilis versi alpha ke internal tester setelah regression test.',
                   NULL,
                   'SEDANG',
                   'BELUM_SELESAI'
            WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE judul_tugas = 'Publikasi Release Alpha')
        """);
    }
}
