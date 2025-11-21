INSERT INTO users (name)
VALUES ('Pomodone User'), ('Guest User')
ON CONFLICT (name) DO NOTHING;
