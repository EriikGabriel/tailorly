-- Segunda opinião da IA sobre o currículo gerado (requisito a requisito). Nula até a pessoa pedir.
ALTER TABLE cv_generations ADD COLUMN ats_review JSONB;
