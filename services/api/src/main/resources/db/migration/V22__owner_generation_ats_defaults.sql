-- Padrões de geração sob ATS de cada conta: otimizar o layout até a nota de ATS ficar boa e priorizar
-- resultados quantificados. Ambos vêm ligados; o layout passa a ser opcional porque a linha agora
-- também existe quando a conta só mexeu nos padrões de ATS.
ALTER TABLE owner_generation_defaults ALTER COLUMN template_name DROP NOT NULL;
ALTER TABLE owner_generation_defaults ADD COLUMN optimize_for_ats BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE owner_generation_defaults ADD COLUMN prioritize_quantified BOOLEAN NOT NULL DEFAULT TRUE;
