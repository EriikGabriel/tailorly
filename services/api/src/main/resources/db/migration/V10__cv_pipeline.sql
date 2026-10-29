-- Templates editados por uma pessoa: pertencem a um owner e são versionados por owner. Os embarcados
-- continuam globais (owner_id nulo). Cada versão continua imutável.
ALTER TABLE cv_templates ADD COLUMN owner_id UUID REFERENCES owners(id);
ALTER TABLE cv_templates DROP CONSTRAINT uk_cv_templates_name_version;
CREATE UNIQUE INDEX uk_cv_templates_bundled_name_version ON cv_templates(name, version) WHERE owner_id IS NULL;
CREATE UNIQUE INDEX uk_cv_templates_owner_name_version ON cv_templates(owner_id, name, version) WHERE owner_id IS NOT NULL;
CREATE INDEX idx_cv_templates_owner_id ON cv_templates(owner_id);

-- A geração passa a registrar o conteúdo final enviado ao template (necessário para a prévia ao
-- vivo), as preferências usadas, o relatório de ATS e as iterações da esteira.
ALTER TABLE cv_generations
    ADD COLUMN pipeline_version VARCHAR(32) NOT NULL DEFAULT 'cv-pipeline-1',
    ADD COLUMN preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN document JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN ats JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE cv_generations
    ALTER COLUMN pipeline_version DROP DEFAULT,
    ALTER COLUMN preferences DROP DEFAULT,
    ALTER COLUMN document DROP DEFAULT,
    ALTER COLUMN ats DROP DEFAULT;
