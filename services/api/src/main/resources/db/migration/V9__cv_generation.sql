-- Templates Typst versionados. O conteúdo de uma versão é imutável (o hash é conferido na
-- inicialização); mudar o layout cria a versão seguinte. Templates formatam, nunca fornecem fatos.
CREATE TABLE cv_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cv_templates_name_version UNIQUE (name, version),
    CONSTRAINT chk_cv_templates_version CHECK (version >= 1)
);

-- Cada geração registra o snapshot do Ground Truth, a versão da vaga e a versão do template usados.
-- Os artefatos (PDF e Markdown) ficam no storage privado e são apagados junto com o owner.
CREATE TABLE cv_generations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    snapshot_id UUID NOT NULL REFERENCES gt_snapshots(id),
    job_posting_version_id UUID NOT NULL REFERENCES job_posting_versions(id),
    template_id UUID NOT NULL REFERENCES cv_templates(id),
    input_hash VARCHAR(64) NOT NULL,
    renderer_version VARCHAR(128) NOT NULL,
    tailoring JSONB NOT NULL,
    pdf_object_id UUID NOT NULL UNIQUE REFERENCES stored_objects(id),
    markdown_object_id UUID NOT NULL UNIQUE REFERENCES stored_objects(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cv_generations_owner_input UNIQUE (owner_id, input_hash)
);

CREATE INDEX idx_cv_generations_owner_created ON cv_generations(owner_id, created_at DESC);
CREATE INDEX idx_cv_generations_snapshot_id ON cv_generations(snapshot_id);
CREATE INDEX idx_cv_generations_job_version_id ON cv_generations(job_posting_version_id);
