package com.tailorly.api.shared.security;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lista e revoga as sessões de refresh token (dispositivos logados) da conta autenticada. */
@RestController
@RequestMapping("/v1/me/sessions")
@RequiredArgsConstructor
public class SessionController {
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;

  @GetMapping
  public List<SessionResponse> list(Authentication authentication) {
    UUID userId = currentUserId(authentication);
    return refreshTokenService.listActive(userId).stream().map(SessionResponse::from).toList();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(Authentication authentication, @PathVariable UUID id) {
    refreshTokenService.revoke(id, currentUserId(authentication));
  }

  private UUID currentUserId(Authentication authentication) {
    User user = userRepository.findByEmail(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
    return user.getId();
  }
}
