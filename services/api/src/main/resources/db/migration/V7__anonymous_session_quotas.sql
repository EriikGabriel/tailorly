-- Contadores de uso por sessão anônima; os limites vêm de tailorly.anonymous.* e são aplicados no servidor.
ALTER TABLE anonymous_sessions
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN uploads_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN extractions_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN generations_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_anonymous_sessions_counts CHECK (
        uploads_count >= 0 AND extractions_count >= 0 AND generations_count >= 0);

-- Apoia a limpeza periódica de sessões expiradas e a contagem de sessões ativas.
CREATE INDEX idx_anonymous_sessions_expires_at ON anonymous_sessions(expires_at);
