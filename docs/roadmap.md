# Roadmap

Estado do produto e o que falta. O que aparece como concluído está implementado e descrito nos
documentos de [produto](product/ground-truth.md); a lista de pendências abaixo é a fonte única do
que **ainda não existe** (os demais documentos só apontam para cá).

## Concluído

1. **Documento no frontend.** Home e `/base` compartilham o mesmo envio e acompanham a extração; o
   painel de texto extraído mostra o resultado bruto por página.
2. **Versão candidata no backend.** O upload cria ou recupera a versão no escopo do owner; reenvio
   idêntico reaproveita a extração e marca a revisão temporal como pendente.
3. **Processamento do PDF.** Fila persistente, PDFBox por página, OCR seletivo com Tesseract e textos
   no storage ([extração](../services/api/docs/extraction.md)).
4. **Fatos e revisão por candidata.** Extração semântica por IA com evidência literal; fatos,
   pendências e decisões auditadas com os estados `pendente`, `confirmado`, `corrigido` e `omitido`.
   Fatos inequívocos entram já confirmados; só os ambíguos geram pendência bloqueante.
5. **Aprovação do Ground Truth.** Snapshot imutável com hash, ativado atomicamente.
6. **Acesso anônimo.** Sessão emitida pelo servidor, limites, TTL, limpeza e migração para conta,
   no servidor e na interface.
7. **Vaga e geração.** Leitura por link ou texto, análise por IA, templates Typst editáveis com
   prévia ao vivo, esteira de ATS, enriquecimento com evidências, histórico, download e exclusão.
8. **Ciclo dos dados.** Desativação, exclusão (inclusive das gerações derivadas) e retenção de contas
   desativadas.
9. **Operação.** Refresh token rotativo com detecção de reuso, CI e deploy (Vercel + Render + Neon +
   Cloudflare R2, ver [Deploy](deploy.md)), due diligence de empresas e exportação opcional para o vault.

## Pendente

**Produto**

- DOC/DOCX como formatos de entrada.
- Evidência desenhada sobre o PDF (bounding boxes) e atualização automática do snapshot depois de
  uma nova extração.
- Idioma e templates adicionais além do português (a escolha entre os quatro layouts já existe).
- Reescrita de formação, certificações e voluntariado e geração de resumo para quem não tem um aprovado.
- Categorizar com IA as tecnologias que o dicionário de agrupamento não conhece.
- Avaliador de ATS externo ou treinado (o atual é por regras).
- Nota própria com backlinks no vault, em vez de só o frontmatter.
- Due diligence: fontes privadas com integração autorizada, atualização periódica e preferência para
  desligar o disparo automático.

**Conta e sessão**

- Recuperação de senha e exclusão da própria conta.
- Política de expiração de tokens por ambiente.
- Histórico de notificações por conta e preferências para silenciá-las.

**Segurança** (o que já existe está em [Segurança](security.md))

- Verificação de e-mail no cadastro e recuperação de senha.
- Autenticação em dois fatores e CAPTCHA no cadastro.
- Limites de taxa compartilhados entre instâncias (hoje em memória, por instância) e bloqueio de IPs abusivos.
- Log de auditoria de ações sensíveis (login, exclusão de dados, mudança de e-mail).
- CSP do frontend em modo bloqueante (hoje só relatório) e varredura estática (CodeQL) no CI.

**Plataforma**

- Coletor automático de objetos órfãos no storage e observabilidade (métricas e logs JSON).
- Isolamento do parser de PDF em processo/container com limite rígido de memória e tempo.
- Fixar o IP resolvido na conexão da leitura de vagas (mitigação completa do SSRF por DNS malicioso).
- Sub-etapas no progresso da leitura da vaga.

**Testes**

- E2E dedicado para desativar e excluir versões, e para a rejeição de arquivos inválidos.
- Suíte integrada com Garage e OCR real.
