DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'task_status'
          AND e.enumlabel = 'TERLAMBAT'
    ) THEN
        CREATE TYPE task_status_new AS ENUM ('BELUM_SELESAI', 'TERLAMBAT', 'SELESAI');

        ALTER TABLE tasks
            ALTER COLUMN status DROP DEFAULT,
            ALTER COLUMN status TYPE task_status_new USING status::text::task_status_new;

        ALTER TABLE tasks
            ALTER COLUMN status SET DEFAULT 'BELUM_SELESAI';

        DROP TYPE task_status;
        ALTER TYPE task_status_new RENAME TO task_status;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION set_task_overdue_status()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.tenggat_tugas IS NOT NULL
        AND NEW.status <> 'SELESAI'
        AND NEW.tenggat_tugas < NOW() THEN
        NEW.status := 'TERLAMBAT';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tasks_overdue_status ON tasks;

CREATE TRIGGER tasks_overdue_status
    BEFORE INSERT OR UPDATE
    ON tasks
    FOR EACH ROW
EXECUTE FUNCTION set_task_overdue_status();
