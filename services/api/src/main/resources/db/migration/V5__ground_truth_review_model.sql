CREATE TABLE fact_records (
    id UUID PRIMARY KEY,
    base_version_id UUID NOT NULL REFERENCES base_versions(id),
    category VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT chk_fact_records_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_fact_records_version_order ON fact_records(base_version_id, display_order);

CREATE TABLE facts (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL REFERENCES fact_records(id),
    extraction_run_id UUID REFERENCES extraction_runs(id),
    field_key VARCHAR(255) NOT NULL,
    extracted_value JSONB,
    reviewed_value JSONB,
    origin VARCHAR(255) NOT NULL,
    review_state VARCHAR(255) NOT NULL,
    confirmed_at TIMESTAMPTZ,
    revalidate_after TIMESTAMPTZ
);

CREATE INDEX idx_facts_record_id ON facts(record_id);
CREATE INDEX idx_facts_extraction_run_id ON facts(extraction_run_id);

CREATE TABLE fact_evidence (
    id UUID PRIMARY KEY,
    fact_id UUID NOT NULL REFERENCES facts(id),
    extracted_page_id UUID NOT NULL REFERENCES extracted_pages(id),
    source_excerpt TEXT NOT NULL,
    source_locator JSONB NOT NULL,
    bounding_box JSONB
);

CREATE INDEX idx_fact_evidence_fact_id ON fact_evidence(fact_id);
CREATE INDEX idx_fact_evidence_page_id ON fact_evidence(extracted_page_id);

CREATE TABLE review_issues (
    id UUID PRIMARY KEY,
    base_version_id UUID NOT NULL REFERENCES base_versions(id),
    fact_id UUID REFERENCES facts(id),
    job_version_id UUID,
    category VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    blocking BOOLEAN NOT NULL,
    status VARCHAR(255) NOT NULL,
    question_and_choices JSONB
);

CREATE INDEX idx_review_issues_version_status ON review_issues(base_version_id, status);
CREATE INDEX idx_review_issues_fact_id ON review_issues(fact_id);

CREATE TABLE review_decisions (
    id UUID PRIMARY KEY,
    base_version_id UUID NOT NULL REFERENCES base_versions(id),
    fact_id UUID REFERENCES facts(id),
    issue_id UUID REFERENCES review_issues(id),
    actor_owner_id UUID NOT NULL REFERENCES owners(id),
    result VARCHAR(255) NOT NULL,
    previous_value JSONB,
    approved_value JSONB,
    review_revision INTEGER NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_review_decisions_revision CHECK (review_revision >= 0)
);

CREATE INDEX idx_review_decisions_version_revision ON review_decisions(base_version_id, review_revision);
CREATE INDEX idx_review_decisions_fact_id ON review_decisions(fact_id);
CREATE INDEX idx_review_decisions_issue_id ON review_decisions(issue_id);

CREATE TABLE gt_snapshots (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    base_version_id UUID NOT NULL REFERENCES base_versions(id),
    snapshot_number INTEGER NOT NULL,
    approved_review_revision INTEGER NOT NULL,
    content_hash VARCHAR(255) NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    schema_version INTEGER NOT NULL,
    CONSTRAINT uk_gt_snapshots_owner_number UNIQUE (owner_id, snapshot_number),
    CONSTRAINT uk_gt_snapshots_id_owner UNIQUE (id, owner_id),
    CONSTRAINT chk_gt_snapshots_number CHECK (snapshot_number > 0),
    CONSTRAINT chk_gt_snapshots_review_revision CHECK (approved_review_revision >= 0),
    CONSTRAINT chk_gt_snapshots_schema_version CHECK (schema_version > 0)
);

CREATE INDEX idx_gt_snapshots_version_id ON gt_snapshots(base_version_id);

CREATE TABLE gt_snapshot_items (
    id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL REFERENCES gt_snapshots(id),
    source_fact_id UUID NOT NULL REFERENCES facts(id),
    source_decision_id UUID NOT NULL REFERENCES review_decisions(id),
    record_id UUID NOT NULL REFERENCES fact_records(id),
    category VARCHAR(255) NOT NULL,
    field_key VARCHAR(255) NOT NULL,
    approved_value JSONB NOT NULL
);

CREATE INDEX idx_gt_snapshot_items_snapshot_id ON gt_snapshot_items(snapshot_id);
CREATE INDEX idx_gt_snapshot_items_source_fact_id ON gt_snapshot_items(source_fact_id);
CREATE INDEX idx_gt_snapshot_items_source_decision_id ON gt_snapshot_items(source_decision_id);
CREATE INDEX idx_gt_snapshot_items_record_id ON gt_snapshot_items(record_id);

ALTER TABLE owners
    ADD CONSTRAINT fk_owners_active_snapshot_owner
    FOREIGN KEY (active_snapshot_id, id) REFERENCES gt_snapshots(id, owner_id);
