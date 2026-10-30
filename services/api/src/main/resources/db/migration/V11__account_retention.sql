ALTER TABLE users ADD COLUMN disabled_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN data_purged_at TIMESTAMPTZ;

CREATE INDEX idx_users_disabled_at ON users (disabled_at) WHERE disabled_at IS NOT NULL;
