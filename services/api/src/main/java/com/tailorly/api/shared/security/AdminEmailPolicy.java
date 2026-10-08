package com.tailorly.api.shared.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
