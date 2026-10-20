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

/**
 * Resolve o usuário autenticado e seu owner único, usado por {@code GET /v1/me}.
 *
 * <p>Contas antigas criadas antes do provisionamento automático de owner não têm um
 * registro em {@code owners}; {@link #findOrCreate} cria o owner sob bloqueio da conta
 * ({@code findByEmailForUpdate}) na primeira chamada, para que o frontend nunca precise
 * persistir um {@code ownerId} separado nem chamar {@code POST /v1/owners} após o login.</p>
 */
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
