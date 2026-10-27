package com.tailorly.api.session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.dto.AnonymousSessionResponse;
import com.tailorly.api.session.dto.AnonymousSessionStatusResponse;
import com.tailorly.api.session.dto.IssuedAnonymousSessionResponse;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissão, autenticação e encerramento de sessões anônimas, sempre no servidor.
 *
 * <p>{@link #issue} cria o owner anônimo e a sessão na mesma transação e devolve o token em
 * texto puro uma única vez; só o hash fica no banco. O TTL da sessão também define
 * {@code owner.expiresAt}, que é herdado pelos objetos temporários e usado pela limpeza
 * ({@link AnonymousCleanupJob}). Cada owner anônimo tem exatamente uma sessão.</p>
 */
@Service
@RequiredArgsConstructor
public class AnonymousSessionService {
  private final AnonymousSessionRepository sessionRepository;
  private final OwnerRepository ownerRepository;
  private final AnonymousProperties properties;
  private final AnonymousDataPurgeService purgeService;

  @Transactional
  public IssuedAnonymousSessionResponse issue() {
    Instant now = Instant.now();
    if (sessionRepository.countByExpiresAtAfterAndRevokedAtIsNull(now) >= properties.maxActiveSessions()) {
      throw new TooManyRequestsException(ErrorCodes.Session.CAPACITY_REACHED);
    }

    Instant expiresAt = now.plus(properties.sessionTtl());
    Owner owner = ownerRepository.save(Owner.builder()
        .kind(OwnerKindEnum.KIND_ANONYMOUS)
        .revision(0)
        .expiresAt(expiresAt)
        .build());

    String token = AnonymousSessionTokens.generate();
    AnonymousSession session = sessionRepository.save(AnonymousSession.builder()
        .owner(owner)
        .tokenHash(AnonymousSessionTokens.hash(token))
        .expiresAt(expiresAt)
        .build());

    return new IssuedAnonymousSessionResponse(session.getId(), owner.getId(), token, expiresAt);
  }

  /** Válida apenas se o token existir, não tiver sido revogado e ainda não tiver expirado. */
  @Transactional(readOnly = true)
  public Optional<AnonymousPrincipal> authenticate(String token) {
    if (!AnonymousSessionTokens.isAnonymousToken(token)) {
      return Optional.empty();
    }
    Instant now = Instant.now();
    return sessionRepository.findByTokenHash(AnonymousSessionTokens.hash(token))
        .filter(session -> session.getRevokedAt() == null)
        .filter(session -> session.getExpiresAt().isAfter(now))
        .map(session -> new AnonymousPrincipal(
            session.getId(), session.getOwner().getId(), session.getExpiresAt()));
  }

  @Transactional(readOnly = true)
  public AnonymousSessionStatusResponse status(AnonymousPrincipal principal) {
    AnonymousSession session = sessionRepository.findById(principal.sessionId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Session.NOT_FOUND));
    return AnonymousSessionStatusResponse.builder()
        .sessionId(session.getId())
        .ownerId(session.getOwner().getId())
        .expiresAt(session.getExpiresAt())
        .migratable(session.getOwner().getActiveSnapshotId() != null)
        .usage(new AnonymousSessionStatusResponse.Usage(
            session.getUploadsCount(), session.getExtractionsCount(), session.getGenerationsCount()))
        .limits(new AnonymousSessionStatusResponse.Limits(
            properties.maxFileBytes(), properties.maxPages(), properties.maxUploads(),
            properties.maxExtractions(), properties.maxGenerations()))
        .build();
  }

  /** Encerra a sessão apagando de imediato todos os dados temporários do visitante. */
  public void destroy(AnonymousPrincipal principal) {
    purgeService.purgeOwner(principal.ownerId());
  }

  @Transactional
  public AnonymousSessionResponse revoke(UUID id) {
    AnonymousSession session = getSession(id);
    if (session.getRevokedAt() == null) {
      session.setRevokedAt(Instant.now());
    }
    return AnonymousSessionResponse.from(sessionRepository.save(session), Instant.now());
  }

  @Transactional(readOnly = true)
  public AnonymousSessionResponse findById(UUID id) {
    return AnonymousSessionResponse.from(getSession(id), Instant.now());
  }

  private AnonymousSession getSession(UUID id) {
    return sessionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Session.NOT_FOUND));
  }
}
