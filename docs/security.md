# Segurança

Resumo do que protege a aplicação hoje, onde está no código e o que ainda falta. Os invariantes de
dados (isolamento por owner, exclusão, sessões anônimas) estão em
[Currículo Base como Ground Truth](product/ground-truth.md); aqui ficam autenticação, limites de uso,
rede e cabeçalhos.

## O que existe

| Área                   | Proteção                                                                                                                                                                                                                                         | Onde                                                         |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------ |
| Senhas                 | BCrypt; 8 a 128 caracteres; a conta é consultada a cada requisição (desativar vale na hora)                                                                                                                                                      | `SecurityConfiguration`, `CreateUserRequest`                 |
| Sessão de conta        | JWT HS256 de 1 h (emissor validado) + refresh token opaco, rotativo, guardado como hash; reuso de token rotacionado revoga todas as sessões                                                                                                      | [Autenticação](../services/api/docs/authentication.md)       |
| Força bruta de login   | Falhas contadas por IP + e-mail (5 em 15 min) e por e-mail (20 em 15 min); sucesso zera a contagem do par. O limite por e-mail é alto de propósito, para não permitir trancar a conta de outra pessoa                                            | `LoginAttemptLimiter`                                        |
| Rate limit por IP      | Teto geral de requisições por IP, 30 tentativas/min em login, refresh e logout e 10 cadastros/h; o health check fica de fora                                                                                                                     | `PublicEndpointRateLimitFilter`                              |
| Rate limit por dono    | 600 req/min por conta e tetos menores nas rotas que gastam IA, baixam páginas externas ou processam PDF (15/min) e na prévia do Typst (60/min); vale também para visitantes                                                                      | `AuthenticatedRateLimitFilter`                               |
| Visitantes             | Token opaco `tlr_anon_` (só o hash no banco), cotas, TTL, limite por sessão e por IP, isolamento por owner                                                                                                                                       | [Acesso anônimo](../services/api/docs/anonymous-sessions.md) |
| SSRF                   | Leitura de vagas só em `http(s)` 80/443, resolve e bloqueia redes internas, revalida cada redirecionamento, timeout e tamanho máximo                                                                                                             | [Vagas](product/job-postings.md#leitura-da-página)           |
| Upload                 | Só PDF (assinatura `%PDF-`), 7 MiB, 20 páginas, PDF criptografado recusado; hash SHA-256 por owner                                                                                                                                               | `BaseDocumentUploadService`                                  |
| Segredos               | Chaves de IA guardadas por conta são criptografadas (`TAILORLY_SECRETS_KEY`); nenhuma chave de OCR/LLM vai ao cliente                                                                                                                            | `SecretEncryptor`                                            |
| CORS                   | Só as origens de `TAILORLY_CORS_ALLOWED_ORIGINS`, sem credenciais (não há cookies), expõe apenas `Retry-After` e `Content-Disposition`                                                                                                           | `WebConfiguration`                                           |
| Cabeçalhos da API      | CSP `default-src 'none'`, `frame-ancestors 'none'`, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, HSTS de 1 ano (em HTTPS) e `Cache-Control: no-store`; o Swagger UI fica fora da CSP | `SecurityConfiguration.securityHeaders`                      |
| Cabeçalhos do frontend | `nosniff`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, HSTS e uma CSP em modo **somente relatório**                                                                                                                              | `apps/web/vercel.json`                                       |
| Dependências           | Dependabot semanal para npm, Maven, Docker e GitHub Actions                                                                                                                                                                                      | `.github/dependabot.yml`                                     |
| Contêiner              | Roda como usuário sem privilégios (`uid 10001`); a documentação OpenAPI vem desligada no Render                                                                                                                                                  | `services/api/Dockerfile`, `render.yaml`                     |

Respostas bloqueadas pelos limites usam `429` com `Retry-After` e a mensagem localizada
`error.session.rate_limited`. Todos os valores são configuráveis ([Configuração](../services/api/docs/configuration.md#rate-limit)).

## Atrás de um proxy (Render)

O IP usado pelos limites é `getRemoteAddr()`. Atrás do proxy do Render ele seria sempre o do proxy,
e todos os visitantes dividiriam o mesmo limite. Por isso o Blueprint define
`TAILORLY_FORWARD_HEADERS_STRATEGY=native`: o Tomcat passa a usar `X-Forwarded-For` **apenas** de
proxies internos (faixas privadas), então um cliente não consegue forjar o cabeçalho. Localmente
(sem proxy) o padrão é `none`. Depois do primeiro deploy, confira que dois navegadores diferentes
têm limites independentes.

Os contadores são em memória e por instância: reiniciar a API (ou o cold start do free tier) zera
todos eles. Para um portfólio de uso pessoal isso é suficiente; veja o roadmap para o caso de várias
instâncias.

## Frontend: CSP em modo relatório

A CSP do `vercel.json` é enviada como `Content-Security-Policy-Report-Only`: o navegador só registra
violações no console, sem bloquear nada, porque o app usa estilos inline (Motion), worker e iframe de
PDF por `blob:` e a origem da API varia. Abra o app em produção com o DevTools, navegue pelas telas
(home, `/base`, `/generator`) e, se não houver violações legítimas, troque a chave para
`Content-Security-Policy` (ajustando `connect-src` ao domínio da API).

## Ainda não existe

Veja a seção de segurança do [roadmap](roadmap.md#pendente): verificação de e-mail, recuperação de
senha, autenticação em dois fatores, CAPTCHA no cadastro, limites compartilhados entre instâncias,
log de auditoria e a CSP em modo bloqueante.
