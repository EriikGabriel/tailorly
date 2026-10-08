package com.tailorly.api.owner;

import java.util.Locale;
import java.util.UUID;

import com.tailorly.api.owner.dto.CreateOwnerRequest;
import com.tailorly.api.owner.dto.OwnerResponse;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerService {
  private final OwnerRepository ownerRepository;
  private final UserRepository userRepository;

  @Transactional
  public OwnerResponse create(CreateOwnerRequest request) {
    OwnerKindEnum kind = parseKind(request.kind());
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean authenticated = authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);

    if (kind == OwnerKindEnum.KIND_ANONYMOUS && request.userId() != null) {
      throw new BadRequestException(ErrorCodes.Owner.USER_ONLY_FOR_ACCOUNT);
    }

    User user = null;
    if (kind == OwnerKindEnum.KIND_ACCOUNT) {
      if (!authenticated) {
        throw new AccessDeniedException(ErrorCodes.Owner.ACCOUNT_AUTH_REQUIRED);
      }
      user = userRepository.findByEmail(authentication.getName())
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
      if (request.userId() != null && !request.userId().equals(user.getId())) {
        throw new AccessDeniedException(ErrorCodes.Owner.ACCOUNT_MISMATCH);
      }
    }

    Owner owner = Owner.builder()
        .kind(kind)
        .user(user)
        .activeSnapshotId(request.activeSnapshotId())
        .revision(0)
        .build();

    return OwnerResponse.from(ownerRepository.save(owner));
  }

  @Transactional(readOnly = true)
  public OwnerResponse findById(UUID id) {
    return OwnerResponse.from(getOwner(id));
  }

  @Transactional
  public OwnerResponse updateActiveSnapshot(UUID id, UUID activeSnapshotId) {
    getOwner(id);
    if (ownerRepository.updateActiveSnapshot(id, activeSnapshotId) == 0) {
      throw new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND);
    }
    return OwnerResponse.from(getOwner(id));
  }

  @Transactional
  public OwnerResponse incrementRevision(UUID id) {
    getOwner(id);
    if (ownerRepository.incrementRevision(id) == 0) {
      throw new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND);
    }
    return OwnerResponse.from(getOwner(id));
  }

  private Owner getOwner(UUID id) {
    Owner owner = ownerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (owner.getKind() != OwnerKindEnum.KIND_ACCOUNT
        || authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || owner.getUser() == null
        || !owner.getUser().getEmail().equals(authentication.getName())) {
      throw new AccessDeniedException(ErrorCodes.Owner.ACCESS_DENIED);
    }
    return owner;
  }

  private OwnerKindEnum parseKind(String kind) {
    if (kind == null) {
      throw new BadRequestException(ErrorCodes.Owner.INVALID_KIND);
    }
    return switch (kind.trim().toLowerCase(Locale.ROOT)) {
      case "account" -> OwnerKindEnum.KIND_ACCOUNT;
      case "anonymous" -> OwnerKindEnum.KIND_ANONYMOUS;
      default -> throw new BadRequestException(ErrorCodes.Owner.INVALID_KIND);
    };
  }
}
