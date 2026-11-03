-- Interruptor da etapa automática de due diligence no perfil da conta. Ligada por padrão; desligada, a
-- leitura de vagas não consulta a web nem a IA para a empresa. A consulta manual na página continua disponível.
-- Uma linha por conta, apagada junto com os demais dados pessoais da conta.
CREATE TABLE owner_due_diligence_preferences (
    owner_id UUID PRIMARY KEY REFERENCES owners(id),
    pipeline_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
);
