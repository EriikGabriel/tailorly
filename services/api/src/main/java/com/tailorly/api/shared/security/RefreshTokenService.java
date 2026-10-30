package com.tailorly.api.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissão, rotação e revogação de refresh tokens, no mesmo padrão de token opaco usado em
 * {@link com.tailorly.api.session.AnonymousSessionTokens} (prefixo fixo, hash SHA-256
 * persistido, texto puro devolvido só na emissão).
 *
 * <p>{@link #rotate} detecta reuso: se o token apresentado já pertence a uma sessão revogada
 * (porque já foi rotacionado ou deslogado antes), toda a cadeia de sessões ativas do usuário é
 * revogada e a tentativa é rejeitada — um refresh token válido só é usado uma vez.</p>
 */
@Service
public class RefreshTokenService {
  private final UserSessionRepository sessions;
  private final UserRepository users;
  private final Duration ttl;

  public RefreshTokenService(
      UserSessionRepository sessions,
      UserRepository users,
      @Value("${tailorly.security.refresh.ttl}") Duration ttl
  ) {
    this.sessions = sessions;
    this.users = users;
    this.ttl = ttl;
  }

  public record Issued(String rawToken, Instant expiresAt) {
  }

  @Transactional
  public Issued issue(UUID userId) {
    return issue(userId, null);
  }

  private Issued issue(UUID userId, UUID rotatedFrom) {
    User user = users.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
    Instant expiresAt = Instant.now().plus(ttl);
    String rawToken = UserSessionTokens.generate();
    sessions.save(UserSession.builder()
        .user(user)
        .tokenHash(UserSessionTokens.hash(rawToken))
        .expiresAt(expiresAt)
        .rotatedFrom(rotatedFrom)
        .build());
    return new Issued(rawToken, expiresAt);
  }

  public record Rotated(UUID userId, Issued issued) {
  }

  @Transactional
  public Rotated rotate(String rawToken) {
    UserSession session = sessions.findByTokenHash(UserSessionTokens.hash(rawToken))
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    UUID userId = session.getUser().getId();
    if (session.getRevokedAt() != null) {
      // O token já foi usado ou revogado antes: trata como roubo e invalida tudo.
      sessions.revokeAllByUserId(userId, Instant.now());
      throw new BadCredentialsException("Refresh token reuse detected");
    }
    if (session.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Refresh token expired");
    }
    session.setRevokedAt(Instant.now());
    sessions.save(session);
    return new Rotated(userId, issue(userId, session.getId()));
  }

  @Transactional
  public void revoke(String rawToken) {
    sessions.findByTokenHash(UserSessionTokens.hash(rawToken))
        .filter(session -> session.getRevokedAt() == null)
        .ifPresent(session -> {
          session.setRevokedAt(Instant.now());
          sessions.save(session);
        });
  }

  @Transactional(readOnly = true)
  public List<UserSession> listActive(UUID userId) {
    return sessions.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public void revoke(UUID sessionId, UUID requestingUserId) {
    UserSession session = sessions.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.RefreshToken.NOT_FOUND));
    if (!session.getUser().getId().equals(requestingUserId)) {
      throw new AccessDeniedException(ErrorCodes.User.ACCESS_DENIED);
    }
    if (session.getRevokedAt() == null) {
      session.setRevokedAt(Instant.now());
      sessions.save(session);
    }
  }
}
