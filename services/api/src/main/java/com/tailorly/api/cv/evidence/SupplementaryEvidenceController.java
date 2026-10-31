package com.tailorly.api.cv.evidence;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.cv.evidence.dto.SupplementaryEvidenceResponse;
import com.tailorly.api.cv.evidence.dto.UpdateEvidenceReviewStateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Coleta, consulta e revisão de evidências complementares de um snapshot do Ground Truth. */
@RestController
@RequestMapping("/v1/cv/evidence")
@RequiredArgsConstructor
public class SupplementaryEvidenceController {
  private final SupplementaryEvidenceService service;

  /** Analisa as fontes (links, busca web e vault) dos projetos do snapshot ainda não analisadas e devolve o resumo. */
  @PostMapping("/snapshots/{snapshotId}/collect")
  public SupplementaryEvidenceService.CollectionResult collect(@PathVariable UUID snapshotId) {
    return service.collect(snapshotId);
  }

  @GetMapping("/snapshots/{snapshotId}")
  public List<SupplementaryEvidenceResponse> findBySnapshot(@PathVariable UUID snapshotId) {
    return service.findBySnapshot(snapshotId);
  }

  /** Confirma, descarta ou devolve a {@code pendente} uma evidência. */
  @PatchMapping("/{id}/review-state")
  public SupplementaryEvidenceResponse updateReviewState(@PathVariable UUID id,
      @Valid @RequestBody UpdateEvidenceReviewStateRequest request) {
    return service.updateReviewState(id, request.reviewState());
  }
}
