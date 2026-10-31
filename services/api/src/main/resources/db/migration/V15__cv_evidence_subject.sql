-- Projeto ao qual a evidência se refere (o link do projeto no Currículo Base). É por ele que o
-- enriquecimento casa evidências com projetos, qualquer que tenha sido a fonte (link do próprio
-- projeto, busca web ou nota do vault). Nulo só para evidências criadas antes desta coluna, que são
-- preenchidas com o próprio link de origem.
ALTER TABLE cv_supplementary_evidence ADD COLUMN subject_url VARCHAR(2048);
UPDATE cv_supplementary_evidence SET subject_url = source_url WHERE source_kind = 'base_link';
