-- Due diligence de empresas (RF08). Cada análise concluída é um registro imutável do owner, com o relatório
-- completo (fatores, fontes, trechos, datas e incerteza). Não referencia o Ground Truth e é apagada com o owner.
CREATE TABLE company_due_diligence (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    company_name VARCHAR(200) NOT NULL,
    job_posting_id UUID,
    job_version_number INTEGER,
    analyzer_version VARCHAR(64) NOT NULL,
    report JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_company_due_diligence_owner_created ON company_due_diligence(owner_id, created_at DESC);
