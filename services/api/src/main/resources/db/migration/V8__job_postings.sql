-- Vagas lidas por link ou texto colado. Cada alteração de conteúdo vira uma nova versão; um reenvio
-- idêntico reaproveita a versão mais recente. Tudo pertence a um owner e é apagado com ele.
CREATE TABLE job_postings (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    source_kind VARCHAR(16) NOT NULL,
    source_key VARCHAR(1100) NOT NULL,
    title VARCHAR(512),
    company_name VARCHAR(512),
    expires_at VARCHAR(64),
    latest_version_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_job_postings_owner_source UNIQUE (owner_id, source_key),
    CONSTRAINT chk_job_postings_source_kind CHECK (source_kind IN ('url', 'text')),
    CONSTRAINT chk_job_postings_latest_version CHECK (latest_version_number >= 1)
);

CREATE INDEX idx_job_postings_owner_updated ON job_postings(owner_id, updated_at DESC);

CREATE TABLE job_posting_versions (
    id UUID PRIMARY KEY,
    job_posting_id UUID NOT NULL REFERENCES job_postings(id),
    version_number INTEGER NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    details JSONB NOT NULL,
    analysis JSONB,
    analysis_status VARCHAR(32) NOT NULL,
    analyzer_version VARCHAR(64),
    analyzed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    last_checked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_job_posting_versions_number UNIQUE (job_posting_id, version_number),
    CONSTRAINT chk_job_posting_versions_number CHECK (version_number >= 1),
    CONSTRAINT chk_job_posting_versions_analysis_status
        CHECK (analysis_status IN ('completed', 'unavailable', 'failed', 'quota_exceeded'))
);
