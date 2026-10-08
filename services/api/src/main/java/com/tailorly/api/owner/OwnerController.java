package com.tailorly.api.owner;

import java.util.UUID;

import com.tailorly.api.owner.dto.CreateOwnerRequest;
import com.tailorly.api.owner.dto.OwnerResponse;
import com.tailorly.api.owner.dto.UpdateActiveSnapshotRequest;
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
@RequestMapping("/v1/owners")
@RequiredArgsConstructor
public class OwnerController {
  private final OwnerService ownerService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OwnerResponse create(@Valid @RequestBody CreateOwnerRequest request) {
    return ownerService.create(request);
  }

  @GetMapping("/{id}")
  public OwnerResponse findById(@PathVariable UUID id) {
    return ownerService.findById(id);
  }

  @PatchMapping("/{id}/active-snapshot")
  public OwnerResponse updateActiveSnapshot(
      @PathVariable UUID id,
      @RequestBody UpdateActiveSnapshotRequest request
  ) {
    return ownerService.updateActiveSnapshot(id, request.activeSnapshotId());
  }

  @PatchMapping("/{id}/increment-revision")
  public OwnerResponse incrementRevision(@PathVariable UUID id) {
    return ownerService.incrementRevision(id);
  }
}
