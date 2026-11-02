package com.tailorly.api.shared.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites de taxa aplicados no servidor ({@code tailorly.rate-limit.*}). Os contadores são em
 * memória e locais a cada instância da API; com várias instâncias o limite efetivo é multiplicado
 * pelo número delas. O IP considerado vem de {@code getRemoteAddr()}: atrás de um proxy reverso,
 * configure {@code server.forward-headers-strategy} (veja {@code TAILORLY_FORWARD_HEADERS_STRATEGY}).
 *
 * @param enabled desliga todos os limites desta classe (não afeta os do acesso anônimo)
 * @param ipRequestsPerMinute teto de requisições por IP, para qualquer rota exceto o health check
 * @param authPerMinute tentativas de login, refresh e logout por IP
 * @param registerPerHour cadastros por IP
 * @param loginFailuresPerIpAndEmail falhas de login toleradas por par IP + e-mail na janela
 * @param loginFailuresPerEmail falhas de login toleradas por e-mail (todos os IPs) na janela
 * @param loginFailureWindow janela de contagem das falhas de login
 * @param accountRequestsPerMinute teto de requisições por conta autenticada
 * @param heavyPerMinute rotas que chamam IA, baixam páginas externas ou processam PDFs, por dono
 * @param previewPerMinute compilações de prévia do Typst, por dono
 */
@ConfigurationProperties(prefix = "tailorly.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("600") int ipRequestsPerMinute,
    @DefaultValue("30") int authPerMinute,
    @DefaultValue("10") int registerPerHour,
    @DefaultValue("5") int loginFailuresPerIpAndEmail,
    @DefaultValue("20") int loginFailuresPerEmail,
    @DefaultValue("PT15M") Duration loginFailureWindow,
    @DefaultValue("600") int accountRequestsPerMinute,
    @DefaultValue("15") int heavyPerMinute,
    @DefaultValue("60") int previewPerMinute
) {
}
