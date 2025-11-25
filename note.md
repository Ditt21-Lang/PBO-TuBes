## Struktur Direktori Pomodone

- `src/main/java/com/pomodone/app`  
  Menyimpan kelas yang menjalankan aplikasi JavaFX. Contoh: `MainApp` memuat scene utama.  
  Padanan Laravel: bootstrap `artisan` + `app/Console/Kernel.php`.  
  - `MainApp.java`: entry point; cek koneksi DB lalu nantinya memuat UI.  
  - `AppLauncher.java`: helper untuk kompatibilitas JavaFX ketika packaging.

- `src/main/java/com/pomodone/config`  
  Berisi konfigurasi aplikasi. `DatabaseConfig` membaca `.env` dan membuat koneksi HikariCP, `AppConfig` tempat menaruh konstanta lain.  
  Padanan Laravel: folder `config/` dan pengaturan `.env`.

- `src/main/java/com/pomodone/model`  
  Representasi data tanpa logika I/O. Misal `model/task/Task`, `TaskBuilder`, `TaskStatus`, `TaskDifficulty`. Tambah POJO lain (pomodoro session, user profile) di sini.  
  Padanan Laravel: `app/Models`.
  - `model/task/Task.java`: struktur tugas immutable (judul, deskripsi, deadline, kesulitan, status, timestamp).  
  - `TaskBuilder.java`: builder untuk membuat `Task`.  
  - `TaskStatus.java`: enum `BELUM_SELESAI`, `TERLAMBAT`, `SELESAI`.  
  - `TaskDifficulty.java`: enum `SULIT`, `MUDAH`, `SEDANG`.  
  - `model/pomodoro/*`: placeholder POJO untuk sesi dan pengaturan pomodoro.  
  - `model/user/UserSettings*.java`: rancangan awal preferensi user (belum terhubung DB).

- `src/main/java/com/pomodone/repository`  
  Kelas untuk CRUD terhadap database. `TaskRepository` akan berisi query SQL insert/update/select tasks, memanfaatkan `DatabaseConfig`.  
  Padanan Laravel: repository/query builder custom (mis. `app/Repositories`).
  - `TaskRepository.java`: tempat tulis method `findAll`, `save`, `updateStatus`. Saat ini masih kosong.  
  - `PomodoroSessionRepository.java`: disiapkan jika nanti sesi pomodoro ingin dipersist.

- `src/main/java/com/pomodone/service`  
  Business logic. `TaskService` memanggil repository, validasi input, dan menyiapkan data untuk UI/facade. Tempat menaruh aturan domain (mis. memeriksa tenggat sebelum membuat tugas baru).  
  Padanan Laravel: `app/Services` atau action class.
  - `TaskService.java`: nantinya koordinasi logika tugas (validasi, atur status).  
  - `PomodoroService.java`: menangani siklus pomodoro (start/stop timer) tanpa DB.  
  - `UserSettingsService.java`: penyimpan preferensi user di memori/file bila diperlukan.

- `src/main/java/com/pomodone/facade`  
  Lapisan penghubung antara service dan controller UI supaya controller tidak menulis banyak koordinasi. Contoh: `TaskManagementFacade` memanggil beberapa service sekaligus.  
  Padanan Laravel: facade/service aggregator (class di `app/Facades` atau service container binding).
  - `TaskManagementFacade.java`: API tinggi untuk create/update/list task.  
  - `PomodoroFacade.java`: expose operasi pomodoro ke UI.  
  - `StudyFocusFacade.java`: gabungkan task + pomodoro (mis. memulai sesi pada task tertentu).

- `src/main/java/com/pomodone/view`  
  Controller JavaFX (.java yang dipasangkan dengan file FXML). `MainViewController`, `TaskListController`, dst menangani event UI.  
  Padanan Laravel: `app/Http/Controllers`.
  - `MainViewController.java`: mengelola scene utama.  
  - `TaskListController.java`: logika tampilan daftar tugas.  
  - `PomodoroController.java`: kontrol timer.  
  - `SettingsController.java`: pengaturan (nama user, preferensi).  
  - `view/util/*`: loader dialog/FXML helper.

- `src/main/java/com/pomodone/event`  
  Kelas event dan event bus sederhana untuk komunikasi antar komponen. `TaskEvent` dikirim ketika task dibuat/diupdate.  
  Padanan Laravel: `app/Events`.
  - `EventBus.java`: publish/subscribe sederhana.  
  - `TaskEvent.java`: payload ketika task berubah.  
  - `PomodoroEvent.java`: pemberitahuan start/stop/finish timer.

- `src/main/java/com/pomodone/strategy`  
  Implementasi strategi yang bisa dipilih/diganti. Contoh: `strategy/task/PriorityTaskSortStrategy` untuk menyortir task, `strategy/pomodoro/ClassicPomodoroStrategy` untuk durasi timer standar.  
  Padanan Laravel: class strategi/helper di `app/Support` atau folder custom.
  - `strategy/task/TaskSortStrategy.java`: interface penyortir task.  
  - `PriorityTaskSortStrategy`, `DeadlineTaskSortStrategy`: implementasi contoh.  
  - `strategy/pomodoro/*`: strategi durasi (Classic/Intense/Custom).  
  - `PomodoroStrategy.java`: interface untuk menentukan lama fokus/break.

- `src/main/java/com/pomodone/util`  
  Helper umum seperti `DateUtils`, `TimeUtils`, `ValidationUtils`. Jangan menaruh logic domain di sini, hanya fungsi pembantu.  
  Padanan Laravel: `app/Helpers` atau fungsi helper global.
  - `DateUtils.java`: format dan parsing tanggal.  
  - `TimeUtils.java`: utilitas timer/durasi.  
  - `ValidationUtils.java`: validasi sederhana (cek string kosong, dsb.).

- `src/main/resources/db/migration`  
  Skrip SQL migrasi dan seeder (V1..V5). Jalankan semuanya dengan `./migration.sh`.  
  Padanan Laravel: `database/migrations` + `database/seeders`.
  - `V1__create_tasks_table.sql`: tabel tasks + enum + trigger updated_at.  
  - `V2__add_overdue_task_status.sql`: enum `TERLAMBAT` + trigger auto-overdue.  
  - `V3__create_users_table.sql`: tabel users (nama saja).  
  - `V4__seed_users.sql`: insert user default.  
  - `V5__seed_tasks.sql`: insert beberapa tugas contoh.

- `src/main/resources/fxml`  
  File layout antarmuka (FXML) yang dipakai controller di paket `view`.  
  Padanan Laravel: Blade template (`resources/views`).

- `migration.sh`  
  Skrip Bash untuk menerapkan seluruh migrasi SQL ke PostgreSQL. Jalankan `bash migration.sh` (atau `./migration.sh` di Unix). Pastikan `psql` tersedia.  
  Padanan Laravel: `php artisan migrate --seed`.

- `build.sh` / `run.sh`  
  Shortcut untuk `mvn clean package` dan `mvn javafx:run`. Pastikan menjalankan `build.sh` minimal sekali supaya dependensi terunduh sebelum `run.sh`.



- app: titik awal program. MainApp akan boot JavaFX lalu menyerahkan kendali ke controller UI.
- config: disiapkan paling awal agar komponen lain bisa ambil konfigurasi (contoh DatabaseConfig diminta oleh repository).
- view: controller JavaFX merespon aksi pengguna; controller tidak langsung ke DB, melainkan memanggil facade.
- facade: merangkum kombinasi operasi service untuk kebutuhan UI tertentu (mis. load daftar tugas + urutannya).
- service: berisi aturan domain; ia menerima input dari facade/controller, memvalidasi, lalu meminta repository membaca/menulis data.
- repository: satu-satunya lapisan yang bicara ke PostgreSQL menggunakan DatabaseConfig.
- model: objek yang dipakai bersama oleh service, facade, view (data task/pomodoro/user). Repository mengembalikan/menerima model ini.
- event/strategy/util: pelengkap—event untuk notifikasi antar komponen, strategy untuk variasi algoritma (sorting task, mode pomodoro), util untuk helper umum.

