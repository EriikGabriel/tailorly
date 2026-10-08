package com.tailorly.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.role.Role;
import com.tailorly.api.role.RoleEnum;
import com.tailorly.api.shared.config.SecurityConfiguration;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import com.tailorly.api.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

class JwtSecurityTest {
  @Test
  void signedTokenIsValidAndTamperingIsRejected() {
    SecurityConfiguration configuration = new SecurityConfiguration(null, null);
    String encodedSecret = Base64.getEncoder().encodeToString(new byte[32]);
    var key = configuration.jwtSecretKey(encodedSecret);
    JwtDecoder decoder = configuration.jwtDecoder(key, "http://localhost:8080");
    UUID userId = UUID.randomUUID();
    LoginResponse login = new JwtTokenService(
        configuration.jwtEncoder(key), "http://localhost:8080", Duration.ofHours(1))
        .issue(new UserResponse(userId, "person@example.com", true, List.of("ROLE_USER")));

    Jwt decoded = decoder.decode(login.accessToken());
    assertEquals(userId.toString(), decoded.getSubject());
    assertEquals("http://localhost:8080", decoded.getIssuer().toString());
    assertEquals("Bearer", login.tokenType());
    assertEquals(login.expiresAt(), decoded.getExpiresAt());
    assertThrows(JwtException.class, () -> decoder.decode(login.accessToken() + "x"));

    String expired = configuration.jwtEncoder(key).encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(),
        JwtClaimsSet.builder()
            .issuer("http://localhost:8080")
            .subject(userId.toString())
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .build())).getTokenValue();
    assertThrows(JwtException.class, () -> decoder.decode(expired));
  }

  @Test
  void bearerUsesCurrentUserStateAndRoles() {
    UserRepository repository = mock(UserRepository.class);
    UUID userId = UUID.randomUUID();
    User user = User.builder()
        .id(userId)
        .email("person@example.com")
        .enabled(true)
        .roles(Set.of(Role.builder().name(RoleEnum.ROLE_ADMIN).build()))
        .build();
    when(repository.findById(userId)).thenReturn(Optional.of(user));
    SecurityConfiguration configuration = new SecurityConfiguration(repository, null);
    Jwt token = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(userId.toString())
        .build();

    var authentication = configuration.jwtAuthenticationConverter().convert(token);
    assertEquals("person@example.com", authentication.getName());
    assertTrue(authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));

    user.setEnabled(false);
    assertThrows(BadCredentialsException.class,
        () -> configuration.jwtAuthenticationConverter().convert(token));
  }

  @Test
  void signingKeyMustBeStrong() {
    SecurityConfiguration configuration = new SecurityConfiguration(null, null);
    assertThrows(IllegalStateException.class,
        () -> configuration.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[16])));
  }
}
