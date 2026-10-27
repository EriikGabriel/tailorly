package com.tailorly.api.owner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

class OwnerAccessServiceTest {
  private final UserRepository users = mock(UserRepository.class);
  private final OwnerAccessService access = new OwnerAccessService(users);

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsSession(UUID ownerId) {
    SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
        new AnonymousPrincipal(UUID.randomUUID(), ownerId, Instant.now().plusSeconds(60))));
  }

  private Owner owner(UUID id, OwnerKindEnum kind) {
    Owner owner = new Owner();
    owner.setId(id);
    owner.setKind(kind);
    return owner;
  }

  @Test
  void sessionAccessesItsOwnAnonymousOwner() {
    UUID ownerId = UUID.randomUUID();
    authenticateAsSession(ownerId);

    assertDoesNotThrow(() -> access.assertCanAccess(owner(ownerId, OwnerKindEnum.KIND_ANONYMOUS)));
    verifyNoInteractions(users);
  }

  @Test
  void sessionCannotAccessAnotherVisitorsOwner() {
    authenticateAsSession(UUID.randomUUID());

    assertThrows(AccessDeniedException.class,
        () -> access.assertCanAccess(owner(UUID.randomUUID(), OwnerKindEnum.KIND_ANONYMOUS)));
  }

  @Test
  void sessionCannotAccessAnAccountOwnerEvenWithTheSameId() {
    UUID ownerId = UUID.randomUUID();
    authenticateAsSession(ownerId);

    assertThrows(AccessDeniedException.class,
        () -> access.assertCanAccess(owner(ownerId, OwnerKindEnum.KIND_ACCOUNT)));
  }

  @Test
  void sessionIsNeverAnAdmin() {
    authenticateAsSession(UUID.randomUUID());

    assertThrows(AccessDeniedException.class, access::assertAdmin);
  }
}
