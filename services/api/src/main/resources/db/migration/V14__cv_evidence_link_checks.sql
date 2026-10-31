-- Marca cada link já analisado de um snapshot, com ou sem detalhes encontrados. Sem essa marca, um
-- link que não gerou detalhes seria analisado de novo (e pago de novo) a cada coleta. Apaga em
-- cascata com o snapshot; o owner é apagado explicitamente pela exclusão de dados pessoais.
CREATE TABLE cv_evidence_link_checks (
    snapshot_id UUID NOT NULL REFERENCES gt_snapshots(id) ON DELETE CASCADE,
    source_url VARCHAR(2048) NOT NULL,
    owner_id UUID NOT NULL REFERENCES owners(id),
    checked_at TIMESTAMPTZ NOT NULL,
    claims_count INTEGER NOT NULL,
    PRIMARY KEY (snapshot_id, source_url),
    CONSTRAINT chk_cv_evidence_link_checks_claims CHECK (claims_count >= 0)
);

CREATE INDEX idx_cv_evidence_link_checks_owner_id ON cv_evidence_link_checks(owner_id);
