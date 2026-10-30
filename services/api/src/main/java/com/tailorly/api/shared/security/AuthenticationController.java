package com.tailorly.api.shared.security;

import com.tailorly.api.user.UserService;
import com.tailorly.api.user.dto.CreateUserRequest;
import com.tailorly.api.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, login, renovação e revogação de sessão. {@link #login} delega a validação de
 * credenciais ao {@link AuthenticationManager} (que usa o {@code AuthenticationProvider}
 * configurado em {@link com.tailorly.api.shared.config.SecurityConfiguration}) e, se válido,
 * emite o par de tokens com {@link JwtTokenService} e {@link RefreshTokenService}.
 *
 * <p>{@link #refresh} e {@link #logout} são públicos ({@code permitAll} em
 * {@code SecurityConfiguration}): a credencial é o próprio refresh token no corpo, não o
 * cabeçalho {@code Authorization} (que pode já estar expirado nesse momento).</p>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenService refreshTokenService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
    UserResponse user = userService.findByEmailForAuthentication(request.email());
    String refreshToken = refreshTokenService.issue(user.id()).rawToken();
    return jwtTokenService.issue(user, refreshToken);
  }

  @PostMapping("/refresh")
  public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
    RefreshTokenService.Rotated rotated = refreshTokenService.rotate(request.refreshToken());
    UserResponse user = userService.findByIdForAuthentication(rotated.userId());
    return jwtTokenService.issue(user, rotated.issued().rawToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshRequest request) {
    refreshTokenService.revoke(request.refreshToken());
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
  }

  public record RefreshRequest(@NotBlank String refreshToken) {
  }
}
