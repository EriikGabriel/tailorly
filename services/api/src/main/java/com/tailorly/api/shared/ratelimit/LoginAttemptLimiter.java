package com.tailorly.api.shared.ratelimit;

import java.util.Locale;

import com.tailorly.api.session.FixedWindowRateLimiter;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

/**
 * Contém a força bruta de senha contando <em>falhas</em> de login. São duas barreiras: por par
 * IP + e-mail (bloqueia quem insiste na mesma conta) e por e-mail (limita ataques distribuídos
 * por vários IPs). O limite por e-mail é bem mais alto que o por IP + e-mail, para que um
 * atacante não consiga trancar a conta da vítima com poucas tentativas. Um login bem-sucedido
 * zera a contagem do par IP + e-mail.
 */
@Component
public class LoginAttemptLimiter {
  private final boolean enabled;
  private final FixedWindowRateLimiter perIpAndEmail;
  private final FixedWindowRateLimiter perEmail;

  public LoginAttemptLimiter(RateLimitProperties properties) {
    this.enabled = properties.enabled();
    this.perIpAndEmail = new FixedWindowRateLimiter(
        properties.loginFailuresPerIpAndEmail(), properties.loginFailureWindow());
    this.perEmail = new FixedWindowRateLimiter(
        properties.loginFailuresPerEmail(), properties.loginFailureWindow());
  }

  /** Lança {@link TooManyRequestsException} se as falhas recentes já esgotaram alguma barreira. */
  public void checkAllowed(String ip, String email) {
    if (enabled && (perIpAndEmail.isExceeded(pairKey(ip, email)) || perEmail.isExceeded(normalize(email)))) {
      throw new TooManyRequestsException(ErrorCodes.Session.RATE_LIMITED);
    }
  }

  public void recordFailure(String ip, String email) {
    if (enabled) {
      perIpAndEmail.tryAcquire(pairKey(ip, email));
      perEmail.tryAcquire(normalize(email));
    }
  }

  public void recordSuccess(String ip, String email) {
    perIpAndEmail.reset(pairKey(ip, email));
  }

  private static String pairKey(String ip, String email) {
    return ip + "|" + normalize(email);
  }

  private static String normalize(String email) {
    return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
  }
}
