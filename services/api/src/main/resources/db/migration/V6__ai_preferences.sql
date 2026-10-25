CREATE TABLE ai_preferences (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL UNIQUE REFERENCES owners(id),
    provider VARCHAR(255) NOT NULL,
    remote_endpoint VARCHAR(2048),
    remote_model VARCHAR(255),
    remote_api_key_encrypted TEXT,
    ollama_endpoint VARCHAR(2048),
    ollama_model VARCHAR(255),
    ollama_api_key_encrypted TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ai_preferences_provider
        CHECK (provider IN ('rules', 'remote', 'remote-with-ollama-fallback', 'ollama-only'))
);
