package com.tailorly.api.owner;

import java.util.Locale;
import java.util.UUID;

import com.tailorly.api.base.version.BaseVersionRepository;
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

/**
 * Criação e consulta de owners. Na prática, a maioria das contas nunca chama {@link #create}
 * diretamente: cadastro e {@code GET /v1/me} provisionam o owner automaticamente (veja
 * {@link com.tailorly.api.shared.security.CurrentAccountService}). Owners anônimos não são
 * criados aqui: nascem junto da sessão emitida por
 * {@link com.tailorly.api.session.AnonymousSessionService}.
 */
@Service
@RequiredArgsConstructor
public class OwnerService {
  private static final String ACTIVE = "ACTIVE";
  private static final String DEACTIVATED = "DEACTIVATED";

  private final OwnerRepository ownerRepository;
  private final UserRepository userRepository;
  private final BaseVersionRepository baseVersionRepository;

  /**
   * Cria um owner. Para {@code KIND_ACCOUNT}, exige autenticação e é idempotente: se a conta
   * já tiver um owner, devolve o existente em vez de criar outro (cada usuário tem no máximo
   * um owner). {@code KIND_ANONYMOUS} é recusado: owners anônimos só são criados pela emissão
   * de uma sessão anônima.
   */
  @Transactional
  public OwnerResponse create(CreateOwnerRequest request) {
    OwnerKindEnum kind = parseKind(request.kind());
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean authenticated = authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);

    // Owners anônimos nascem apenas junto da sessão emitida pelo servidor (AnonymousSessionService).
    if (kind == OwnerKindEnum.KIND_ANONYMOUS) {
      throw new BadRequestException(ErrorCodes.Anonymous.ANONYMOUS_OWNER_CREATION_FORBIDDEN);
    }

    User user = null;
    if (kind == OwnerKindEnum.KIND_ACCOUNT) {
      if (!authenticated) {
        throw new AccessDeniedException(ErrorCodes.Owner.ACCOUNT_AUTH_REQUIRED);
      }
      user = userRepository.findByEmailForUpdate(authentication.getName())
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
      if (request.userId() != null && !request.userId().equals(user.getId())) {
        throw new AccessDeniedException(ErrorCodes.Owner.ACCOUNT_MISMATCH);
      }
      var existingOwner = ownerRepository.findByUserId(user.getId());
      if (existingOwner.isPresent()) {
        return OwnerResponse.from(existingOwner.get());
      }
    }

    Owner owner = Owner.builder()
        .kind(kind)
        .user(user)
        .activeSnapshotId(null)
        .revision(0)
        .build();

    return OwnerResponse.from(ownerRepository.save(owner));
  }

  @Transactional(readOnly = true)
  public OwnerResponse findById(UUID id) {
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

  /**
   * Desativa o Ground Truth ativo do owner: zera {@code activeSnapshotId} e marca a versão que
   * estava {@code ACTIVE} como {@code DEACTIVATED}. Diferente da exclusão de versão, nada é
   * apagado — o snapshot, os fatos e as pendências continuam disponíveis para auditoria, e um
   * novo snapshot aprovado no futuro volta a ativar o Ground Truth.
   */
  @Transactional
  public OwnerResponse deactivateActiveSnapshot(UUID id) {
    Owner owner = getOwner(id);
    if (owner.getActiveSnapshotId() == null) {
      throw new BadRequestException(ErrorCodes.Owner.NO_ACTIVE_SNAPSHOT);
    }

    baseVersionRepository.updateStatusByOwnerAndStatus(id, ACTIVE, DEACTIVATED);
    if (ownerRepository.updateActiveSnapshot(id, null) == 0) {
      throw new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND);
    }
    ownerRepository.incrementRevision(id);

    return OwnerResponse.from(getOwner(id));
  }

  private Owner getOwner(UUID id) {
    Owner owner = ownerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    assertCanAccess(owner);
    return owner;
  }

  private void assertCanAccess(Owner owner) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (owner.getKind() != OwnerKindEnum.KIND_ACCOUNT
        || authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || owner.getUser() == null
        || !owner.getUser().getEmail().equals(authentication.getName())) {
      throw new AccessDeniedException(ErrorCodes.Owner.ACCESS_DENIED);
    }
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
