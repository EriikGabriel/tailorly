-- Vault Obsidian de cada conta: um caminho relativo à pasta raiz configurada no servidor
-- (tailorly.vault.root). Uma conta tem no máximo um vault. Contas apagadas perdem a linha junto com
-- os demais dados pessoais (OwnerDataEraser); a validação do caminho é feita pela aplicação.
CREATE TABLE owner_vaults (
    owner_id UUID PRIMARY KEY REFERENCES owners(id),
    relative_path VARCHAR(1024) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
