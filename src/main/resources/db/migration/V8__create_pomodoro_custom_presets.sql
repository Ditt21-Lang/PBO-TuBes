CREATE TABLE IF NOT EXISTS pomodoro_custom_presets (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preset_name          VARCHAR(100) NOT NULL DEFAULT 'Custom',
    focus_minutes        INTEGER NOT NULL,
    short_break_minutes  INTEGER NOT NULL,
    long_break_minutes   INTEGER NOT NULL,
    rounds               INTEGER NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, preset_name)
);

DROP TRIGGER IF EXISTS pomo_custom_presets_updated_at ON pomodoro_custom_presets;
CREATE TRIGGER pomo_custom_presets_updated_at
    BEFORE UPDATE ON pomodoro_custom_presets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
