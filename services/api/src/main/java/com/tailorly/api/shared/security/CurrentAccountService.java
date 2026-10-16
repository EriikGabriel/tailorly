package com.tailorly.api.shared.security;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.owner.dto.OwnerResponse;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import com.tailorly.api.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentAccountService {
  private final UserRepository userRepository;
  private final OwnerRepository ownerRepository;

  @Transactional
  public CurrentAccountResponse findOrCreate(Authentication authentication) {
    User user = userRepository.findByEmailForUpdate(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.User.NOT_FOUND));

    Owner owner = ownerRepository.findByUserId(user.getId())
        .orElseGet(() -> ownerRepository.save(Owner.builder()
            .kind(OwnerKindEnum.KIND_ACCOUNT)
            .user(user)
            .revision(0)
            .build()));

    return new CurrentAccountResponse(UserResponse.from(user), OwnerResponse.from(owner));
  }
}
