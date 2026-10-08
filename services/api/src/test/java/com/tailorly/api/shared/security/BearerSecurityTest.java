package com.tailorly.api.shared.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.role.Role;
import com.tailorly.api.role.RoleEnum;
import com.tailorly.api.shared.config.SecurityConfiguration;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserController;
import com.tailorly.api.user.UserRepository;
import com.tailorly.api.user.UserService;
import com.tailorly.api.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@WebMvcTest({UserController.class, AuthenticationController.class})
@Import({SecurityConfiguration.class, LocalizedSecurityErrorHandler.class, JwtTokenService.class})
@TestPropertySource(properties = {
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.jwt.issuer=http://localhost:8080",
    "tailorly.security.jwt.ttl=PT1H"
})
class BearerSecurityTest {
  @Autowired
  private MockMvc mvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserService userService;

  @Test
  void loginChecksPasswordAndReturnsSignedToken() throws Exception {
    UUID id = UUID.randomUUID();
    User user = User.builder()
        .id(id)
        .email("person@example.com")
        .passwordHash(passwordEncoder.encode("secret123"))
        .enabled(true)
        .roles(Set.of(Role.builder().name(RoleEnum.ROLE_USER).build()))
        .build();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(userService.findByEmailForAuthentication(user.getEmail())).thenReturn(
        new UserResponse(id, user.getEmail(), true, List.of("ROLE_USER")));

    mvc.perform(post("/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"person@example.com\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
    mvc.perform(post("/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"person@example.com\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedRouteRequiresBearerAndRejectsDisabledAccount() throws Exception {
    UUID id = UUID.randomUUID();
    User user = User.builder()
        .id(id)
        .email("person@example.com")
        .enabled(true)
        .roles(Set.of(Role.builder().name(RoleEnum.ROLE_USER).build()))
        .build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userService.findById(id)).thenReturn(
        new UserResponse(id, user.getEmail(), true, List.of("ROLE_USER")));
    String token = jwtTokenService.issue(
        new UserResponse(id, user.getEmail(), true, List.of("ROLE_USER"))).accessToken();

    mvc.perform(get("/v1/users/{id}", id))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/users/{id}", id).header("Authorization", "Basic cGVyc29uOnBhc3M="))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/users/{id}", id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("person@example.com"));

    user.setEnabled(false);
    mvc.perform(get("/v1/users/{id}", id).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }
}
