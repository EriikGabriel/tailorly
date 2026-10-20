ALTER TABLE extraction_runs ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Registros legados de metadados não participam do pipeline executável.
CREATE UNIQUE INDEX uk_extraction_reusable_pipeline
    ON extraction_runs(base_version_id, pipeline_fingerprint)
    WHERE status IN ('QUEUED', 'PROCESSING', 'COMPLETED')
      AND pipeline_versions->>'implementation' = 'selective-pdf-v1';

CREATE INDEX idx_extraction_queue ON extraction_runs(status, created_at);
