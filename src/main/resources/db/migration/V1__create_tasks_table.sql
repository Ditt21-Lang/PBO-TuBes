DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
                 JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'task_difficulty'
    ) THEN
        CREATE TYPE task_difficulty AS ENUM ('SULIT', 'MUDAH', 'SEDANG');
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
                 JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'task_status'
    ) THEN
        CREATE TYPE task_status AS ENUM ('SELESAI', 'BELUM_SELESAI');
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS tasks (
    id              BIGSERIAL PRIMARY KEY,
    judul_tugas     VARCHAR(255)        NOT NULL,
    deskripsi_tugas TEXT,
    tenggat_tugas   TIMESTAMP WITH TIME ZONE,
    tingkat_kesulitan task_difficulty   NOT NULL,
    status            task_status       NOT NULL DEFAULT 'BELUM_SELESAI',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tasks_updated_at ON tasks;

CREATE TRIGGER tasks_updated_at
    BEFORE UPDATE
    ON tasks
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
