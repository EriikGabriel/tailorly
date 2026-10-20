package com.tailorly.api.base;

import java.util.UUID;

import com.tailorly.api.base.dto.BaseVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/base-versions", "/v1/base-versions"})
@RequiredArgsConstructor
public class BaseVersionController {
  private final BaseVersionService baseVersionService;

  @GetMapping("/{id}")
  public BaseVersionResponse findById(@PathVariable UUID id) {
    return baseVersionService.findById(id);
  }
}
