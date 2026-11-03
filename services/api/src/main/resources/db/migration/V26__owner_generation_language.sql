-- Idioma do currículo final, escolhido no perfil: `auto` (detecta pela vaga, português do Brasil ou
-- inglês), `pt-BR` ou `en`. Contas existentes e sem linha gravada seguem em `auto`.
ALTER TABLE owner_generation_defaults ADD COLUMN language VARCHAR(8) NOT NULL DEFAULT 'auto';
ALTER TABLE owner_generation_defaults ADD CONSTRAINT owner_generation_defaults_language_check
    CHECK (language IN ('auto', 'pt-BR', 'en'));

-- Repositórios do GitHub da pessoa viram evidências complementares de projetos candidatos (`github`).
ALTER TABLE cv_supplementary_evidence DROP CONSTRAINT IF EXISTS chk_cv_supplementary_evidence_kind;
ALTER TABLE cv_supplementary_evidence ADD CONSTRAINT chk_cv_supplementary_evidence_kind
    CHECK (source_kind IN ('base_link', 'web', 'vault', 'github'));
