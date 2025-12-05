# PomoDone

JavaFX app untuk manajemen tugas dan Pomodoro, dengan tone santai tapi tetap rapi. Dashboard merangkum produktivitas, Task List bisa filter/sort/cari, dan timer Pomodoro mendukung mode Classic, Intense, serta Custom. Data disimpan via repository ke SQLite (default) atau Postgres jika dikonfigurasi.

## Fitur Utama
- Dashboard: ringkasan sesi, tugas aktif, produktivitas, dan 5 tugas prioritas terdekat.
- Task List: filter status, sort nama/tenggat/difficulty, cari judul/deskripsi, tambah/edit/hapus/mark done, dialog tambah bisa dipicu dari dashboard.
- Pomodoro: mode Classic/Intense/Custom, validasi input, progress bar, alarm, auto-log sesi ke DB, simpan preset custom terakhir.
- Profil ringkas: sapaan pakai nama user + target Pomodoro harian/mingguan dari DB.

## Prasyarat
- JDK 21 (pastikan `JAVA_HOME` sudah mengarah ke JDK, bukan JRE).
- Maven 3.9+.
- Opsional: PostgreSQL 14+ jika ingin memakai DB server; SQLite sudah dibundel.
- Opsional: `jpackage` (JDK 17+) + rpm/dpkg/rpmbuild jika ingin membuat installer native via `release.sh`.

## Cara Jalan Cepat
1. Jalankan aplikasi: `./run.sh` (akan mengarahkan cache JavaFX ke `.javafx-cache/` agar tidak kena permission issue). Alternatif: `mvn javafx:run`.
2. Data lokal default disimpan di SQLite dengan path: `~/Library/Application Support/pomodone/pomodone.db` (macOS), `%APPDATA%\\pomodone\\pomodone.db` (Windows), atau `~/.local/share/pomodone/pomodone.db` (Linux). Flyway otomatis menjalankan migrasi + seed sample data.
3. Tes: `mvn test` (DB test diarahkan ke `jdbc:sqlite:target/test.db`, native access JavaFX diizinkan).
4. Build jar: `./build.sh` atau `mvn package`.

## Konfigurasi Database
- Override via env/.env:
  ```env
  DB_URL=jdbc:postgresql://localhost:5432/pomodone
  DB_USER=postgres
  DB_PASSWORD=secret
  ```
- Jika koneksi ke DB non-SQLite gagal, app otomatis fallback ke SQLite default (log peringatan akan muncul).
- Lokasi migrasi: `src/main/resources/db/migration` (dijalankan otomatis oleh Flyway saat startup).
- Untuk menjalankan migrasi SQL manual ke Postgres: `./migration.sh` (membaca `.env`, butuh `psql`).

## Struktur Proyek
- `src/main/resources/fxml` — layout UI.
- `src/main/java/com/pomodone/view` — controller JavaFX.
- `src/main/java/com/pomodone/service` — logika bisnis (tugas, pomodoro, stats, preset, sesi).
- `src/main/java/com/pomodone/repository` — akses DB (menggunakan `DatabaseConfig` + Flyway).
- `src/main/java/com/pomodone/model` — model domain.
- `src/main/resources/db/migration` — skema + seed SQL.
- `src/test/java` — unit test sort/filter, service, model, preset/sesi, stats, pomodoro.

## Arsitektur Singkat
- Layering: View (FXML + controller) ↔ Service (aturan bisnis) ↔ Repository (SQL) ↔ DB.
- Pattern: Facade (`TaskManagementFacade`, `PomodoroFacade`) untuk sederhanakan akses UI; Strategy (sort tugas, mode Pomodoro); Singleton (`DatabaseConfig`, `PomodoroService`) untuk resource global; Builder di `Task`; Repository untuk persistence.
- Alur Dashboard: controller panggil `DashboardStatsService` + `TaskService.getTopByDueDate`, data masuk ke kartu dan daftar prioritas; search diteruskan ke Task List via `SearchContext`.
- Alur Task List: checkbox/radio + `CollectionViewProcessor` + strategi sort → tampil ListView; dialog add/edit via `TaskService`/`TaskManagementFacade`.
- Alur Pomodoro: controller bind ke `PomodoroService` (timer + state machine), mode ditentukan Strategy; sesi selesai dilog via `PomodoroSessionService`, preset custom disimpan lewat `CustomPomodoroPresetService`.

## Catatan UI
- Tombol "+ New Task" di Dashboard langsung pindah ke Task List dan membuka dialog tambah.
- Pencarian di Dashboard diteruskan ke Task List lewat `SearchContext`.

## Distribusi Native
- `./release.sh` membuat paket via `jpackage` ke folder `dist/` (otomatis pilih tipe paket sesuai OS; override dengan `JPACKAGE_TYPE=app-image|dmg|msi|deb|rpm`).
- Butuh `jpackage` di PATH serta tool tambahan (`rpmbuild` untuk rpm, `dpkg-deb`/`fakeroot` untuk deb).

## Kontribusi
- Issue/ide/bug report selalu diterima.
- Pull request dipersilakan; komentar kode santai, bahasa Indonesia, to the point.
- Pastikan `mvn test` hijau sebelum kirim. Jika butuh DB lain, set `DB_URL` sesuai.

## Roadmap Ringkas
- Test integrasi repository dengan Testcontainers.
- Test UI (TaskList/Dashboard) via TestFX.
- Notifikasi desktop saat sesi Pomodoro selesai.
