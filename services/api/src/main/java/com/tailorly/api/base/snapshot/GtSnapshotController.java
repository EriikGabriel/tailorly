package com.tailorly.api.base.snapshot;

import java.util.UUID;

import com.tailorly.api.base.snapshot.dto.GtSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/gt-snapshots", "/v1/gt-snapshots"})
@RequiredArgsConstructor
/** Rotas para consultar e aprovar snapshots imutáveis do Ground Truth. */
public class GtSnapshotController {
  private final GtSnapshotService service;

  @GetMapping("/{id}")
  public GtSnapshotResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/approve-version/{baseVersionId}")
  @ResponseStatus(HttpStatus.CREATED)
  public GtSnapshotResponse approveVersion(@PathVariable UUID baseVersionId) {
    return service.approve(baseVersionId);
  }
}
