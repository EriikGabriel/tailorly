ALTER TABLE base_versions
    ADD COLUMN temporal_review_required BOOLEAN NOT NULL DEFAULT TRUE;
