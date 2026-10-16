CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE owners (
    id UUID PRIMARY KEY,
    kind VARCHAR(255) NOT NULL,
    user_id UUID UNIQUE REFERENCES users(id),
    active_snapshot_id UUID,
    revision INTEGER NOT NULL,
    expires_at TIMESTAMPTZ,
    CONSTRAINT chk_owners_kind_user CHECK (
        (kind = 'KIND_ACCOUNT' AND user_id IS NOT NULL)
        OR (kind = 'KIND_ANONYMOUS' AND user_id IS NULL)
    )
);

CREATE TABLE anonymous_sessions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL UNIQUE REFERENCES owners(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE stored_objects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    purpose VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_hash VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    expires_at TIMESTAMPTZ,
    deletion_status VARCHAR(255) NOT NULL
);

CREATE INDEX idx_stored_objects_owner_id ON stored_objects(owner_id);

CREATE TABLE base_documents (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    original_object_id UUID NOT NULL UNIQUE REFERENCES stored_objects(id),
    content_hash VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    page_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_base_documents_owner_content_hash UNIQUE (owner_id, content_hash),
    CONSTRAINT chk_base_documents_page_count CHECK (page_count IS NULL OR page_count > 0)
);

CREATE INDEX idx_base_documents_owner_id ON base_documents(owner_id);

CREATE TABLE base_versions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id),
    document_id UUID NOT NULL REFERENCES base_documents(id),
    version_number INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    review_revision INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_base_versions_owner_version_number UNIQUE (owner_id, version_number),
    CONSTRAINT chk_base_versions_version_number CHECK (version_number > 0),
    CONSTRAINT chk_base_versions_review_revision CHECK (review_revision >= 0)
);

CREATE INDEX idx_base_versions_document_id ON base_versions(document_id);

CREATE TABLE extraction_runs (
    id UUID PRIMARY KEY,
    base_version_id UUID NOT NULL REFERENCES base_versions(id),
    pipeline_fingerprint VARCHAR(255) NOT NULL,
    pipeline_versions JSONB NOT NULL,
    output_object_id UUID UNIQUE REFERENCES stored_objects(id),
    status VARCHAR(255) NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_code VARCHAR(255)
);

CREATE INDEX idx_extraction_runs_base_version_id ON extraction_runs(base_version_id);

CREATE TABLE extracted_pages (
    id UUID PRIMARY KEY,
    extraction_run_id UUID NOT NULL REFERENCES extraction_runs(id),
    page_number INTEGER,
    extraction_method VARCHAR(255) NOT NULL,
    text_object_id UUID NOT NULL UNIQUE REFERENCES stored_objects(id),
    text_hash VARCHAR(255) NOT NULL,
    CONSTRAINT uk_extracted_pages_run_page UNIQUE (extraction_run_id, page_number),
    CONSTRAINT chk_extracted_pages_page_number CHECK (page_number IS NULL OR page_number > 0)
);

CREATE INDEX idx_extracted_pages_run_id ON extracted_pages(extraction_run_id);
CREATE INDEX idx_extracted_pages_text_hash ON extracted_pages(text_hash);
