package com.tailorly.api.shared.security;

import java.util.HashSet;

import com.tailorly.api.role.Role;
import com.tailorly.api.role.RoleEnum;
import com.tailorly.api.role.RoleRepository;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final AdminEmailPolicy adminEmailPolicy;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Role userRole = findOrCreate(RoleEnum.ROLE_USER);
    Role adminRole = findOrCreate(RoleEnum.ROLE_ADMIN);

    userRepository.findAll().forEach(user -> {
      if (user.getRoles() == null) {
        user.setRoles(new HashSet<>());
      }

      boolean changed = user.getRoles().add(userRole);
      if (adminEmailPolicy.isAdmin(user.getEmail())) {
        changed |= user.getRoles().add(adminRole);
      }

      if (changed) {
        userRepository.save(user);
      }
    });
  }

  private Role findOrCreate(RoleEnum name) {
    return roleRepository.findByName(name)
        .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
  }
}
