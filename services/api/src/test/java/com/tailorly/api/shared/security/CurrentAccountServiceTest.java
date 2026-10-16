package com.tailorly.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.role.Role;
import com.tailorly.api.role.RoleEnum;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CurrentAccountServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private OwnerRepository ownerRepository;

  private CurrentAccountService service;
  private User user;
  private UsernamePasswordAuthenticationToken authentication;

  @BeforeEach
  void setUp() {
    service = new CurrentAccountService(userRepository, ownerRepository);
    user = User.builder()
        .id(UUID.randomUUID())
        .email("person@example.com")
        .enabled(true)
        .roles(Set.of(Role.builder().name(RoleEnum.ROLE_USER).build()))
        .build();
    authentication = UsernamePasswordAuthenticationToken.authenticated(
        user.getEmail(), null, Set.of());
    when(userRepository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
  }

  @Test
  void returnsExistingOwnerWithoutCreatingAnother() {
    Owner owner = accountOwner();
    when(ownerRepository.findByUserId(user.getId())).thenReturn(Optional.of(owner));

    CurrentAccountResponse response = service.findOrCreate(authentication);

    assertEquals(user.getId(), response.user().id());
    assertEquals(owner.getId(), response.owner().id());
    verify(ownerRepository, never()).save(any());
  }

  @Test
  void provisionsOwnerForAccountCreatedBeforeOwnerProvisioning() {
    when(ownerRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
    when(ownerRepository.save(any())).thenAnswer(invocation -> {
      Owner owner = invocation.getArgument(0);
      owner.setId(UUID.randomUUID());
      return owner;
    });

    CurrentAccountResponse response = service.findOrCreate(authentication);

    assertEquals("account", response.owner().kind());
    assertEquals(user.getId(), response.owner().userId());
    verify(ownerRepository).save(any(Owner.class));
  }

  private Owner accountOwner() {
    return Owner.builder()
        .id(UUID.randomUUID())
        .kind(OwnerKindEnum.KIND_ACCOUNT)
        .user(user)
        .revision(0)
        .build();
  }
}
