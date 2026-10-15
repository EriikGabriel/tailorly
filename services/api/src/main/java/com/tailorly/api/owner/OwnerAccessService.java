package com.tailorly.api.owner;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OwnerAccessService {
  private final UserRepository userRepository;

  public void assertCanAccess(Owner owner) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AccessDeniedException(ErrorCodes.Security.AUTHENTICATION_REQUIRED);
    }

    if (authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
      return;
    }

    User user = userRepository.findByEmail(authentication.getName())
        .orElseThrow(() -> new AccessDeniedException(ErrorCodes.Security.ACCESS_DENIED));

    if (owner.getUser() == null || !user.getId().equals(owner.getUser().getId())) {
      throw new AccessDeniedException(ErrorCodes.Owner.ACCESS_DENIED);
    }
  }

  public void assertAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || authentication.getAuthorities().stream()
        .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
      throw new AccessDeniedException(ErrorCodes.Security.ACCESS_DENIED);
    }
  }
}
