# Tailorly — instruções para agentes

Estas instruções valem para o monorepo. Leia o `README.md`, especialmente “Decisão de produto: Currículo Base como Ground Truth”, antes de alterar ingestão, extração, revisão, geração, persistência ou autenticação. Diferencie sempre o fluxo alvo da implementação atual; não apresente dados de exemplo como currículos reais de um visitante.

## Invariantes do domínio

- O Currículo Base revisado é a fonte dos fatos. Templates Typst formatam a saída; vagas e fontes secundárias não podem inventar ou substituir fatos pessoais confirmados.
- Há um Ground Truth **ativo** por conta ou sessão anônima. Trocar arquivo cria uma versão candidata; a versão ativa anterior continua válida até a candidata concluir extração e pendências obrigatórias. Ative e desative versões de forma atômica.
- Compare o hash do conteúdo no escopo da mesma conta ou sessão antes de processar. Reenvio idêntico reaproveita OCR, extração e fatos sem duplicação. Não deduplique entre pessoas. Reavalie fatos sensíveis ao tempo mesmo quando o arquivo é idêntico.
- Extraia texto diretamente quando possível e use OCR quando necessário. Guarde evidência de origem por fato, versões do pipeline e estados de revisão (`pendente`, `confirmado`, `corrigido`, `omitido`). Vincule pendências e respostas à versão do Currículo Base. Nunca sobrescreva correções do usuário silenciosamente em um reprocessamento.
- Gere currículos apenas a partir do snapshot de fatos aprovado e registre as versões do Ground Truth e da vaga usadas. Gerações subsequentes não devem reexecutar OCR do mesmo arquivo.
- Diferencie desativação de exclusão. Exclusão deve abranger arquivo, texto extraído, fatos e pendências da versão; verifique também artefatos gerados que contenham esses dados antes de prometer exclusão completa.

## Sessões anônimas

- Visitantes podem usar o app com limites de tamanho/páginas, processamento, gerações e taxa de requisições aplicados no servidor. Uma sessão anônima pode ter storage temporário com TTL, mesmo sem registro durável de usuário.
- `sessionStorage` ou estado React são apenas estado de interface, nunca fonte de verdade para fatos, cache de processamento ou cotas. Não exponha chaves de OCR/LLM no cliente.
- Expiração ou remoção apaga dados temporários da sessão. Se a pessoa criar conta antes da expiração, ofereça migração do snapshot já revisado sem repetir OCR. Sem migração, não mantenha histórico persistente do visitante.
- Mostre estado vazio ou de prévia para visitantes sem documentos reais; não apresente exemplos como histórico pessoal.

Ao implementar, mantenha a documentação sincronizada com o comportamento efetivo e marque claramente o que ainda é planejado.
