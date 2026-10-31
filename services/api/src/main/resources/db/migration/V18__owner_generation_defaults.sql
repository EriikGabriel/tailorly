-- Padrões de geração de cada conta (hoje, o layout de currículo escolhido). Uma linha por conta;
-- sessões anônimas não têm padrões persistidos. Apagada junto com os demais dados pessoais da conta.
CREATE TABLE owner_generation_defaults (
    owner_id UUID PRIMARY KEY REFERENCES owners(id),
    template_name VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
