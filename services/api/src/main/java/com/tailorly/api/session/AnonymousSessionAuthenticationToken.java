package com.tailorly.api.session;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Autenticação de uma sessão anônima. Tem uma authority própria
 * ({@link #AUTHORITY}), diferente de qualquer role de conta, e {@code getName()} nunca coincide
 * com um e-mail, então código que compara o nome autenticado com usuários nunca a trata como
 * uma conta.
 */
public class AnonymousSessionAuthenticationToken extends AbstractAuthenticationToken {
  public static final String AUTHORITY = "ROLE_ANONYMOUS_SESSION";

  private final AnonymousPrincipal principal;

  public AnonymousSessionAuthenticationToken(AnonymousPrincipal principal) {
    super(List.of(new SimpleGrantedAuthority(AUTHORITY)));
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public AnonymousPrincipal getPrincipal() {
    return principal;
  }

  @Override
  public String getName() {
    return "anonymous-session:" + principal.sessionId();
  }
}
