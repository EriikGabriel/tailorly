package com.tailorly.api.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
public class CurrentAccountController {
  private final CurrentAccountService currentAccountService;

  @GetMapping
  public CurrentAccountResponse current(Authentication authentication) {
    return currentAccountService.findOrCreate(authentication);
  }
}
