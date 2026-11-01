-- Etapa atual do processamento (leitura, extração/OCR, interpretação por IA...), para a interface
-- mostrar o que está acontecendo em vez de só "processando". Só é preenchida enquanto a execução roda.
ALTER TABLE extraction_runs ADD COLUMN stage VARCHAR(32);
ALTER TABLE extraction_runs ADD COLUMN stage_detail VARCHAR(200);
