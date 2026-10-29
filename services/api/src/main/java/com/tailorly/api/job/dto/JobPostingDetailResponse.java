package com.tailorly.api.job.dto;

import java.time.Instant;
import java.util.List;

public record JobPostingDetailResponse(
    JobPostingSummaryResponse posting,
    JobPostingVersionResponse latest,
    List<VersionInfo> versions
) {
  public record VersionInfo(int versionNumber, String contentHash, String analysisStatus, Instant createdAt) {
  }
}
