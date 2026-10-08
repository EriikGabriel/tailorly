package com.tailorly.api.user;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.role.Role;
import com.tailorly.api.role.RoleEnum;
import com.tailorly.api.role.RoleRepository;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.security.AdminEmailPolicy;
import com.tailorly.api.user.dto.CreateUserRequest;
import com.tailorly.api.user.dto.UpdateUserRequest;
import com.tailorly.api.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final AdminEmailPolicy adminEmailPolicy;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse create(@NonNull CreateUserRequest requestUser) {
    if (userRepository.existsByEmail(requestUser.email())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.User.EMAIL_ALREADY_REGISTERED);
    }

    HashSet<Role> roles = new HashSet<>();
    roles.add(getRole(RoleEnum.ROLE_USER));
    if (adminEmailPolicy.isAdmin(requestUser.email())) {
      roles.add(getRole(RoleEnum.ROLE_ADMIN));
    }

    User user = User.builder()
        .email(requestUser.email())
        .passwordHash(passwordEncoder.encode(requestUser.password()))
        .enabled(true)
        .roles(roles)
        .build();

    User savedUser = userRepository.save(user);

    return UserResponse.from(savedUser);
  }

  public List<UserResponse> findAll() {
    return userRepository.findAll()
        .stream()
        .map(UserResponse::from)
        .toList();
  }

  public UserResponse findById(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
    requireSelf(user.getEmail());
    return UserResponse.from(user);
  }

  public UserResponse findByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
    requireSelf(user.getEmail());
    return UserResponse.from(user);
  }

  public UserResponse findByEmailForAuthentication(String email) {
    return userRepository.findByEmail(email)
        .map(UserResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));
  }

  @Transactional
  public UserResponse update(UUID id, @NonNull UpdateUserRequest requestUser) {
    User user = userRepository.findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND)
        );
    requireSelf(user.getEmail());

    if (!user.getEmail().equals(requestUser.email()) && userRepository.existsByEmail(requestUser.email())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.User.EMAIL_ALREADY_REGISTERED);
    }

    user.setEmail(requestUser.email());
    if (adminEmailPolicy.isAdmin(requestUser.email())) {
      user.getRoles().add(getRole(RoleEnum.ROLE_ADMIN));
    }

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void disable(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND)
        );

    requireSelf(user.getEmail());
    user.setEnabled(false);
  }

  private void requireSelf(String email) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = authentication != null
        && authentication.getAuthorities().stream()
        .anyMatch(authority -> RoleEnum.ROLE_ADMIN.name().equals(authority.getAuthority()));

    if (authentication == null
        || !authentication.isAuthenticated()
        || (!isAdmin && !email.equals(authentication.getName()))) {
      throw new AccessDeniedException(ErrorCodes.User.ACCESS_DENIED);
    }
  }

  private Role getRole(RoleEnum name) {
    return roleRepository.findByName(name)
        .orElseThrow(() -> new IllegalStateException("Role not initialized: " + name));
  }
}
