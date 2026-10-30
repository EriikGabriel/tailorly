package com.tailorly.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
  @Mock
  private UserSessionRepository sessions;

  @Mock
  private UserRepository users;

  private RefreshTokenService service;
  private User user;

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(sessions, users, Duration.ofDays(30));
    user = User.builder().id(UUID.randomUUID()).email("person@example.com").enabled(true).build();
  }

  @Test
  void issueSavesHashedTokenAndReturnsRawTokenOnce() {
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenService.Issued issued = service.issue(user.getId());

    assertTrue(issued.rawToken().startsWith(UserSessionTokens.PREFIX));
    ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
    verify(sessions).save(captor.capture());
    assertEquals(UserSessionTokens.hash(issued.rawToken()), captor.getValue().getTokenHash());
  }

  @Test
  void rotateRevokesOldSessionAndIssuesNewOne() {
    UserSession active = UserSession.builder()
        .id(UUID.randomUUID())
        .user(user)
        .tokenHash(UserSessionTokens.hash("tlr_rt_old"))
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
    when(sessions.findByTokenHash(UserSessionTokens.hash("tlr_rt_old"))).thenReturn(Optional.of(active));
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenService.Rotated rotated = service.rotate("tlr_rt_old");

    assertEquals(user.getId(), rotated.userId());
    assertTrue(rotated.issued().rawToken().startsWith(UserSessionTokens.PREFIX));
    assertTrue(active.getRevokedAt() != null);
  }

  @Test
  void rotateWithAlreadyRevokedTokenRevokesEverythingAndRejects() {
    UserSession reused = UserSession.builder()
        .id(UUID.randomUUID())
        .user(user)
        .tokenHash(UserSessionTokens.hash("tlr_rt_stolen"))
        .expiresAt(Instant.now().plusSeconds(60))
        .revokedAt(Instant.now().minusSeconds(5))
        .build();
    when(sessions.findByTokenHash(UserSessionTokens.hash("tlr_rt_stolen"))).thenReturn(Optional.of(reused));

    assertThrows(BadCredentialsException.class, () -> service.rotate("tlr_rt_stolen"));

    verify(sessions).revokeAllByUserId(eq(user.getId()), any());
  }

  @Test
  void rotateWithUnknownTokenIsRejected() {
    when(sessions.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class, () -> service.rotate("tlr_rt_unknown"));
  }

  @Test
  void revokeBySessionIdRejectsAnotherUser() {
    UUID otherUserId = UUID.randomUUID();
    UserSession session = UserSession.builder().id(UUID.randomUUID()).user(user).build();
    when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

    assertThrows(AccessDeniedException.class, () -> service.revoke(session.getId(), otherUserId));
  }
}
