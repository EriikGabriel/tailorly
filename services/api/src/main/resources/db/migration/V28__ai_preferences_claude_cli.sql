-- Provider local `claude-cli` (Claude Code logado na máquina do servidor) nas preferências da conta.
-- Só é aceito quando o servidor o habilita (TAILORLY_CLAUDE_CLI_ENABLED); o executável continua sendo
-- configuração exclusiva do servidor. claude_cli_efforts guarda o esforço por etapa
-- ({"default": "medium", "interpretation": "high", ...}); etapas ausentes usam o padrão do servidor.
-- IF EXISTS: bancos criados antes de a V6 ter o CHECK não têm a restrição.
ALTER TABLE ai_preferences DROP CONSTRAINT IF EXISTS chk_ai_preferences_provider;
ALTER TABLE ai_preferences ADD CONSTRAINT chk_ai_preferences_provider
    CHECK (provider IN ('rules', 'remote', 'remote-with-ollama-fallback', 'ollama-only', 'claude-cli'));

ALTER TABLE ai_preferences ADD COLUMN claude_cli_model VARCHAR(255);
ALTER TABLE ai_preferences ADD COLUMN claude_cli_timeout_seconds INTEGER
    CHECK (claude_cli_timeout_seconds IS NULL OR claude_cli_timeout_seconds BETWEEN 10 AND 1800);
ALTER TABLE ai_preferences ADD COLUMN claude_cli_efforts JSONB;
