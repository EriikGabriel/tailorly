-- Diretrizes de destaque padrão de cada conta, no lugar do interruptor único de resultados
-- quantificados da V22: agora o perfil guarda a lista inteira (JSON) e o gerador parte dela por vaga.
-- Quem tinha desligado os resultados quantificados mantém as outras duas diretrizes padrão.
ALTER TABLE owner_generation_defaults ADD COLUMN guidelines TEXT NOT NULL
    DEFAULT '["Destacar Liderança Técnica","Arquitetura em Nuvem & Escala","Impacto de Negócio Quantificado"]';
UPDATE owner_generation_defaults
    SET guidelines = '["Destacar Liderança Técnica","Arquitetura em Nuvem & Escala"]'
    WHERE NOT prioritize_quantified;
ALTER TABLE owner_generation_defaults DROP COLUMN prioritize_quantified;
