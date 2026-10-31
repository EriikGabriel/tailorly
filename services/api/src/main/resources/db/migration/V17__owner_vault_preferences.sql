-- Preferências de segundo cérebro de cada conta, na mesma tabela do vault. O caminho passa a ser
-- opcional: a conta pode ligar a busca web sem ter vault. Leitura e exportação exigem caminho (a
-- aplicação valida). A subpasta de exportação nula usa o padrão do servidor (tailorly.vault.subfolder).
ALTER TABLE owner_vaults ALTER COLUMN relative_path DROP NOT NULL;
ALTER TABLE owner_vaults
    ADD COLUMN read_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN export_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN export_subfolder VARCHAR(512),
    ADD COLUMN web_search_enabled BOOLEAN NOT NULL DEFAULT TRUE;
