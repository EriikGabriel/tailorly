ALTER TABLE owners DROP CONSTRAINT IF EXISTS chk_owners_kind_user;

ALTER TABLE owners ADD CONSTRAINT chk_owners_kind_user CHECK (
    (kind = 'KIND_ACCOUNT' AND user_id IS NOT NULL)
    OR (kind = 'KIND_ANONYMOUS' AND user_id IS NULL)
);
