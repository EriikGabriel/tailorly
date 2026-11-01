-- Item do currículo a que a evidência se refere (ex.: projects:2, experience:0) e um rótulo legível.
-- Permite enriquecer também projetos sem link e experiências, que antes não tinham como ser casados.
-- Evidências antigas continuam casando pelo link do projeto (subject_url).
ALTER TABLE cv_supplementary_evidence ADD COLUMN subject_key VARCHAR(64);
ALTER TABLE cv_supplementary_evidence ADD COLUMN subject_label VARCHAR(200);
