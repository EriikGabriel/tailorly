package com.tailorly.api.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import com.tailorly.api.shared.handler.GlobalExceptionHandler;
import com.tailorly.api.user.UserService;
import com.tailorly.api.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {
  @Mock
  private UserService userService;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtTokenService jwtTokenService;

  @Mock
  private RefreshTokenService refreshTokenService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    StaticMessageSource messages = new StaticMessageSource();
    mvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(
            userService, authenticationManager, jwtTokenService, refreshTokenService))
        .setControllerAdvice(new GlobalExceptionHandler(messages))
        .build();
  }

  @Test
  void registerReturnsCreatedUserWithoutPassword() throws Exception {
    UUID id = UUID.randomUUID();
    when(userService.create(any())).thenReturn(
        new UserResponse(id, "person@example.com", true, List.of("ROLE_USER")));

    mvc.perform(post("/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"person@example.com\",\"password\":\"secret123\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.email").value("person@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void loginReturnsUserAfterAuthentication() throws Exception {
    UUID userId = UUID.randomUUID();
    when(authenticationManager.authenticate(any())).thenReturn(
        UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of()));
    when(userService.findByEmailForAuthentication("person@example.com")).thenReturn(
        new UserResponse(userId, "person@example.com", true, List.of("ROLE_USER")));
    when(refreshTokenService.issue(userId)).thenReturn(
        new RefreshTokenService.Issued("tlr_rt_abc", Instant.parse("2030-02-01T00:00:00Z")));
    when(jwtTokenService.issue(any(), any())).thenAnswer(invocation -> new LoginResponse(
        "signed-token", "Bearer", Instant.parse("2030-01-01T00:00:00Z"),
        invocation.getArgument(0), invocation.getArgument(1)));

    mvc.perform(post("/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"person@example.com\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("signed-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.refreshToken").value("tlr_rt_abc"))
        .andExpect(jsonPath("$.user.email").value("person@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void loginRejectsBadCredentialsWithoutLookingUpUser() throws Exception {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));

    mvc.perform(post("/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"person@example.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());

    verify(userService, never()).findByEmailForAuthentication(any());
    verify(jwtTokenService, never()).issue(any(), any());
  }

  @Test
  void refreshRotatesTokenAndReturnsNewPair() throws Exception {
    UUID userId = UUID.randomUUID();
    when(refreshTokenService.rotate("tlr_rt_old")).thenReturn(new RefreshTokenService.Rotated(
        userId, new RefreshTokenService.Issued("tlr_rt_new", Instant.parse("2030-02-01T00:00:00Z"))));
    when(userService.findByIdForAuthentication(userId)).thenReturn(
        new UserResponse(userId, "person@example.com", true, List.of("ROLE_USER")));
    when(jwtTokenService.issue(any(), any())).thenAnswer(invocation -> new LoginResponse(
        "signed-token-2", "Bearer", Instant.parse("2030-01-01T01:00:00Z"),
        invocation.getArgument(0), invocation.getArgument(1)));

    mvc.perform(post("/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"tlr_rt_old\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("signed-token-2"))
        .andExpect(jsonPath("$.refreshToken").value("tlr_rt_new"));
  }

  @Test
  void logoutRevokesTokenAndReturnsNoContent() throws Exception {
    mvc.perform(post("/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"tlr_rt_old\"}"))
        .andExpect(status().isNoContent());

    verify(refreshTokenService).revoke("tlr_rt_old");
  }
}
