-- A due diligence também roda sozinha na esteira (depois do Ground Truth, ao ler uma vaga), em segundo plano:
-- o registro nasce 'running' e termina 'completed' (com relatório) ou 'failed' (sem relatório).
-- 'trigger_kind' distingue o pedido manual do disparo automático da esteira.
ALTER TABLE company_due_diligence ALTER COLUMN report DROP NOT NULL;
ALTER TABLE company_due_diligence ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'completed';
ALTER TABLE company_due_diligence ADD COLUMN trigger_kind VARCHAR(16) NOT NULL DEFAULT 'manual';
ALTER TABLE company_due_diligence ADD CONSTRAINT chk_company_due_diligence_status
    CHECK (status IN ('running', 'completed', 'failed'));
ALTER TABLE company_due_diligence ADD CONSTRAINT chk_company_due_diligence_trigger
    CHECK (trigger_kind IN ('manual', 'pipeline'));
ALTER TABLE company_due_diligence ADD CONSTRAINT chk_company_due_diligence_report
    CHECK (status <> 'completed' OR report IS NOT NULL);
