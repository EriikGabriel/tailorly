-- Evidências complementares: detalhes encontrados fora do Currículo Base (links de projetos, notas
-- do vault, páginas) para enriquecer uma geração. Não são fatos do Ground Truth: cada evidência
-- guarda a fonte, o trecho citado e o estado de revisão, e só entra no currículo quando confirmada.
-- Ligada ao snapshot (versão do Ground Truth) e, depois de usada, à geração. Apagar o snapshot ou a
-- geração apaga as evidências junto, para que a exclusão cubra esses dados.
CREATE TABLE cv_supplementary_evidence (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    snapshot_id UUID NOT NULL REFERENCES gt_snapshots(id) ON DELETE CASCADE,
    generation_id UUID REFERENCES cv_generations(id) ON DELETE CASCADE,
    source_kind VARCHAR(32) NOT NULL,
    source_url VARCHAR(2048),
    source_excerpt TEXT NOT NULL,
    claim TEXT NOT NULL,
    review_state VARCHAR(16) NOT NULL DEFAULT 'pendente',
    collected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_cv_supplementary_evidence_kind
        CHECK (source_kind IN ('base_link', 'web', 'vault')),
    CONSTRAINT chk_cv_supplementary_evidence_state
        CHECK (review_state IN ('pendente', 'confirmado', 'descartado'))
);

CREATE INDEX idx_cv_supplementary_evidence_owner_id ON cv_supplementary_evidence(owner_id);
CREATE INDEX idx_cv_supplementary_evidence_snapshot_id ON cv_supplementary_evidence(snapshot_id);
CREATE INDEX idx_cv_supplementary_evidence_generation_id ON cv_supplementary_evidence(generation_id);
