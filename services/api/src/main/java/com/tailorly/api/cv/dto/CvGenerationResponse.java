package com.tailorly.api.cv.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.cv.CvGeneration;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.pipeline.CvAtsResult;
import com.tailorly.api.cv.template.TypstTemplateMaterializer;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.objects.StoredObject;

/**
 * Um currículo gerado e suas origens. {@code reused} é verdadeiro quando a entrada era idêntica a
 * uma geração existente e nenhum arquivo novo foi produzido.
 */
public record CvGenerationResponse(
    UUID id,
    UUID ownerId,
    GroundTruthSource groundTruth,
    JobPostingSource job,
    TemplateSource template,
    CvPreferences preferences,
    CvAtsResult ats,
    String rendererVersion,
    List<String> matchedTerms,
    String analysisStatus,
    GeneratedFile pdf,
    GeneratedFile markdown,
    Instant createdAt,
    boolean reused
) {
  public record GroundTruthSource(UUID snapshotId, int snapshotNumber, String contentHash, UUID baseVersionId) {
  }

  public record JobPostingSource(UUID jobPostingId, UUID versionId, int versionNumber, String contentHash,
      String title, String companyName) {
  }

  public record TemplateSource(UUID id, String name, int version, boolean custom, String content) {
    public TemplateSource(UUID id, String name, int version, boolean custom) {
      this(id, name, version, custom, null);
    }
  }

  public record GeneratedFile(String format, String mimeType, long sizeBytes, String contentHash) {
    static GeneratedFile of(String format, StoredObject object) {
      return new GeneratedFile(format, object.getMimeType(), object.getSizeBytes(), object.getContentHash());
    }
  }

  public static CvGenerationResponse from(CvGeneration generation, boolean reused) {
    JobPostingVersion job = generation.getJobPostingVersion();
    var snapshot = generation.getSnapshot();
    return new CvGenerationResponse(
        generation.getId(),
        generation.getOwner().getId(),
        new GroundTruthSource(snapshot.getId(), snapshot.getSnapshotNumber(), snapshot.getContentHash(),
            snapshot.getBaseVersion().getId()),
        new JobPostingSource(job.getJobPosting().getId(), job.getId(), job.getVersionNumber(), job.getContentHash(),
            job.getDetails().title(),
            job.getDetails().company() == null ? null : job.getDetails().company().name()),
        new TemplateSource(generation.getTemplate().getId(), generation.getTemplate().getName(),
            generation.getTemplate().getVersion(), generation.getTemplate().getOwner() != null,
            TypstTemplateMaterializer.materialize(generation.getTemplate().getContent(), generation.getDocument())),
        generation.getPreferences(),
        generation.getAts(),
        generation.getRendererVersion(),
        generation.getTailoring().matchedTerms(),
        generation.getTailoring().analysisStatus(),
        GeneratedFile.of("pdf", generation.getPdfObject()),
        GeneratedFile.of("markdown", generation.getMarkdownObject()),
        generation.getCreatedAt(),
        reused);
  }
}
