-- Provider local `codex-cli` (Codex CLI logado com conta ChatGPT na máquina do servidor) nas preferências
-- da conta. Só é aceito quando o servidor o habilita (TAILORLY_CODEX_CLI_ENABLED); o executável continua
-- sendo configuração exclusiva do servidor. Mesma forma das colunas claude_cli_* (V28).
ALTER TABLE ai_preferences DROP CONSTRAINT IF EXISTS chk_ai_preferences_provider;
ALTER TABLE ai_preferences ADD CONSTRAINT chk_ai_preferences_provider
    CHECK (provider IN ('rules', 'remote', 'remote-with-ollama-fallback', 'ollama-only', 'claude-cli', 'codex-cli'));

ALTER TABLE ai_preferences ADD COLUMN codex_cli_model VARCHAR(255);
ALTER TABLE ai_preferences ADD COLUMN codex_cli_timeout_seconds INTEGER
    CHECK (codex_cli_timeout_seconds IS NULL OR codex_cli_timeout_seconds BETWEEN 10 AND 1800);
ALTER TABLE ai_preferences ADD COLUMN codex_cli_efforts JSONB;
