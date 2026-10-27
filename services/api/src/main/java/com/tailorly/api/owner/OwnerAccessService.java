package com.tailorly.api.owner;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.user.User;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Regra de autorização central para dados ligados a um {@link Owner}: só o dóno da conta ou
 * um {@code ROLE_ADMIN} pode acessar os documentos, versões e execuções desse owner.
 *
 * <p>Chamado por todos os serviços que leem/escrevem dados de um owner específico (upload,
 * versões, execuções de extração, objetos armazenados), para que a regra de acesso fique em
 * um único lugar em vez de repetida em cada controller/service.</p>
 */
@Service
@RequiredArgsConstructor
public class OwnerAccessService {
  private final UserRepository userRepository;

  /**
   * Autoriza o acesso de administradores e do próprio dono da conta; rejeita sessões anônimas
   * não autenticadas e contas diferentes do owner informado.
   */
  public void assertCanAccess(Owner owner) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AccessDeniedException(ErrorCodes.Security.AUTHENTICATION_REQUIRED);
    }

    // Uma sessão anônima só acessa o próprio owner anônimo, nunca o de uma conta.
    if (authentication instanceof AnonymousSessionAuthenticationToken session) {
      if (owner.getKind() == OwnerKindEnum.KIND_ANONYMOUS
          && owner.getId().equals(session.getPrincipal().ownerId())) {
        return;
      }
      throw new AccessDeniedException(ErrorCodes.Owner.ACCESS_DENIED);
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

  /** Restringe a operação inteira a {@code ROLE_ADMIN}, usado pelas consultas administrativas globais. */
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
