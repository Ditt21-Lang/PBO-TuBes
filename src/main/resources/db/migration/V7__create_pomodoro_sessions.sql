DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'pomodoro_session_status'
    ) THEN
        CREATE TYPE pomodoro_session_status AS ENUM ('COMPLETED', 'CANCELLED');
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'pomodoro_mode'
    ) THEN
        CREATE TYPE pomodoro_mode AS ENUM ('CLASSIC', 'INTENSE', 'CUSTOM');
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS pomodoro_sessions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    mode             pomodoro_mode NOT NULL,
    status           pomodoro_session_status NOT NULL DEFAULT 'COMPLETED',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pomo_sessions_user_started
    ON pomodoro_sessions (user_id, started_at DESC);
