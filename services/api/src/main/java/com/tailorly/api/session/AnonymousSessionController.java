package com.tailorly.api.session;

import java.util.UUID;

import com.tailorly.api.session.dto.AnonymousSessionResponse;
import com.tailorly.api.session.dto.AnonymousSessionValidationResponse;
import com.tailorly.api.session.dto.CreateAnonymousSessionRequest;
import com.tailorly.api.session.dto.ValidateAnonymousSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/anonymous-sessions")
@RequiredArgsConstructor
public class AnonymousSessionController {
  private final AnonymousSessionService sessionService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AnonymousSessionResponse create(
      @Valid @RequestBody CreateAnonymousSessionRequest request
  ) {
    return sessionService.create(request);
  }

  @PostMapping("/validate")
  public AnonymousSessionValidationResponse validate(
      @Valid @RequestBody ValidateAnonymousSessionRequest request
  ) {
    return sessionService.validate(request.tokenHash());
  }

  @PatchMapping("/{id}/revoke")
  public AnonymousSessionResponse revoke(@PathVariable UUID id) {
    return sessionService.revoke(id);
  }

  @GetMapping("/{id}")
  public AnonymousSessionResponse findById(@PathVariable UUID id) {
    return sessionService.findById(id);
  }
}
