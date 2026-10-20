package com.tailorly.api.shared.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lista de e-mails de administradores locais, configurada por {@code TAILORLY_ADMIN_EMAILS}
 * (separados por vírgula). Usada por {@link RoleInitializer} na inicialização para conceder
 * {@code ROLE_ADMIN} a contas existentes ou recém-criadas cujo e-mail esteja na lista; nunca
 * remove roles já persistidas.
 */
@Component
public class AdminEmailPolicy {
  private final Set<String> adminEmails;

  public AdminEmailPolicy(
      @Value("${tailorly.security.admin-emails:}") String configuredAdminEmails
  ) {
    this.adminEmails = Arrays.stream(configuredAdminEmails.split(","))
        .map(String::trim)
        .filter(email -> !email.isEmpty())
        .map(email -> email.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }

  public boolean isAdmin(String email) {
    return email != null && adminEmails.contains(email.toLowerCase(Locale.ROOT));
  }
}
