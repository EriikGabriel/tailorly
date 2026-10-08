package com.tailorly.api.session;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.dto.AnonymousSessionResponse;
import com.tailorly.api.session.dto.AnonymousSessionValidationResponse;
import com.tailorly.api.session.dto.CreateAnonymousSessionRequest;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnonymousSessionService {
  private final AnonymousSessionRepository sessionRepository;
  private final OwnerRepository ownerRepository;

  @Transactional
  public AnonymousSessionResponse create(CreateAnonymousSessionRequest request) {
    Owner owner = ownerRepository.findById(request.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    if (owner.getKind() != OwnerKindEnum.KIND_ANONYMOUS) {
      throw new BadRequestException(ErrorCodes.Session.OWNER_MUST_BE_ANONYMOUS);
    }
    if (!request.expiresAt().isAfter(Instant.now())) {
      throw new BadRequestException(ErrorCodes.Session.EXPIRATION_MUST_BE_FUTURE);
    }
    if (sessionRepository.existsByOwnerId(owner.getId())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.Session.OWNER_ALREADY_HAS_SESSION);
    }
    if (sessionRepository.findByTokenHash(request.tokenHash()).isPresent()) {
      throw new ResourceAlreadyExistsException(ErrorCodes.Session.TOKEN_ALREADY_EXISTS);
    }

    AnonymousSession session = AnonymousSession.builder()
        .owner(owner)
        .tokenHash(request.tokenHash())
        .expiresAt(request.expiresAt())
        .build();
    owner.setExpiresAt(request.expiresAt());
    return AnonymousSessionResponse.from(sessionRepository.save(session), Instant.now());
  }

  @Transactional(readOnly = true)
  public AnonymousSessionValidationResponse validate(String tokenHash) {
    return sessionRepository.findByTokenHash(tokenHash)
        .filter(session -> session.getRevokedAt() == null)
        .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
        .map(session -> AnonymousSessionValidationResponse.builder()
            .valid(true)
            .ownerId(session.getOwner().getId())
            .expiresAt(session.getExpiresAt())
            .build())
        .orElseGet(AnonymousSessionValidationResponse::invalid);
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
