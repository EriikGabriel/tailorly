package com.tailorly.api.base.fact.group;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.group.dto.CreateFactRecordRequest;
import com.tailorly.api.base.fact.group.dto.FactRecordResponse;
import com.tailorly.api.base.fact.group.dto.UpdateDisplayOrderRequest;
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
@RequestMapping({"/api/fact-records", "/v1/fact-records"})
@RequiredArgsConstructor
public class FactRecordController {
  private final FactRecordService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FactRecordResponse create(@Valid @RequestBody CreateFactRecordRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public FactRecordResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @GetMapping("/version/{baseVersionId}")
  public List<FactRecordResponse> findByVersion(@PathVariable UUID baseVersionId) {
    return service.findByVersion(baseVersionId);
  }

  @PatchMapping("/{id}/display-order")
  public FactRecordResponse updateDisplayOrder(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateDisplayOrderRequest request
  ) {
    return service.updateDisplayOrder(id, request);
  }
}
