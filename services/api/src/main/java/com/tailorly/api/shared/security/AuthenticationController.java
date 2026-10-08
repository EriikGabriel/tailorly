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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenService jwtTokenService;

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
    return jwtTokenService.issue(user);
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
  }
}
