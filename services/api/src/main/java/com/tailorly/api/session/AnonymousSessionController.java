package com.tailorly.api.session;

import java.util.UUID;

import com.tailorly.api.session.dto.AnonymousMigrationResponse;
import com.tailorly.api.session.dto.AnonymousSessionResponse;
import com.tailorly.api.session.dto.AnonymousSessionStatusResponse;
import com.tailorly.api.session.dto.IssuedAnonymousSessionResponse;
import com.tailorly.api.session.dto.MigrateAnonymousSessionRequest;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessões anônimas: emissão pública (limitada por IP), consulta e encerramento pela própria
 * sessão, migração para conta (exige conta autenticada) e consulta/revogação administrativas.
 */
@RestController
@RequestMapping("/v1/anonymous-sessions")
public class AnonymousSessionController {
  private final AnonymousSessionService sessionService;
  private final AnonymousMigrationService migrationService;
  private final FixedWindowRateLimiter issueLimiter;

  public AnonymousSessionController(
      AnonymousSessionService sessionService,
      AnonymousMigrationService migrationService,
      @Qualifier("anonymousSessionIssueLimiter") FixedWindowRateLimiter issueLimiter
  ) {
    this.sessionService = sessionService;
    this.migrationService = migrationService;
    this.issueLimiter = issueLimiter;
  }

  /**
   * Emite uma sessão anônima. O IP vem de {@code getRemoteAddr()}; atrás de um proxy reverso,
   * configure {@code server.forward-headers-strategy} para que ele reflita o cliente real.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IssuedAnonymousSessionResponse create(HttpServletRequest request) {
    if (!issueLimiter.tryAcquire(request.getRemoteAddr())) {
      throw new TooManyRequestsException(ErrorCodes.Session.RATE_LIMITED);
    }
    return sessionService.issue();
  }

  @GetMapping("/current")
  public AnonymousSessionStatusResponse current(@AuthenticationPrincipal Object principal) {
    return sessionService.status(requireSession(principal));
  }

  /** Encerra a sessão e apaga de imediato os dados temporários do visitante. */
  @DeleteMapping("/current")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void end(@AuthenticationPrincipal Object principal) {
    sessionService.destroy(requireSession(principal));
  }

  /** Move o snapshot revisado da sessão anônima para a conta autenticada, sem repetir OCR. */
  @PostMapping("/migrate")
  public AnonymousMigrationResponse migrate(Authentication authentication,
      @Valid @RequestBody MigrateAnonymousSessionRequest request) {
    return migrationService.migrate(authentication, request);
  }

  @PatchMapping("/{id}/revoke")
  public AnonymousSessionResponse revoke(@PathVariable UUID id) {
    return sessionService.revoke(id);
  }

  @GetMapping("/{id}")
  public AnonymousSessionResponse findById(@PathVariable UUID id) {
    return sessionService.findById(id);
  }

  private static AnonymousPrincipal requireSession(Object principal) {
    if (principal instanceof AnonymousPrincipal session) {
      return session;
    }
    throw new AccessDeniedException(ErrorCodes.Session.SESSION_REQUIRED);
  }
}
