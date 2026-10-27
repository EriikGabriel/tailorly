package com.tailorly.api.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnonymousQuotaServiceTest {
  private static final AnonymousProperties PROPERTIES = new AnonymousProperties(
      Duration.ofHours(1), 1000, 2, 3, 2, 1, 60, 10, 100, Duration.ofMinutes(5), true);

  @Mock AnonymousSessionRepository sessions;

  private final UUID ownerId = UUID.randomUUID();

  private AnonymousQuotaService service() {
    return new AnonymousQuotaService(sessions, PROPERTIES);
  }

  private Owner owner(OwnerKindEnum kind) {
    Owner owner = new Owner();
    owner.setId(ownerId);
    owner.setKind(kind);
    return owner;
  }

  @Test
  void rejectsFilesAboveTheAnonymousSizeLimit() {
    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service().assertFileSize(1001));

    assertEquals(ErrorCodes.Anonymous.FILE_TOO_LARGE, error.getMessage());
    assertDoesNotThrow(() -> service().assertFileSize(1000));
  }

  @Test
  void countsPdfPagesAndEnforcesTheLimit() throws Exception {
    assertEquals(2, service().assertPageLimit(pdf(2)));

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service().assertPageLimit(pdf(3)));
    assertEquals(ErrorCodes.Anonymous.PAGE_LIMIT_EXCEEDED, error.getMessage());
  }

  @Test
  void rejectsUnreadablePdfs() {
    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service().assertPageLimit("%PDF-garbage".getBytes()));

    assertEquals(ErrorCodes.BaseDocument.INVALID_PDF, error.getMessage());
  }

  @Test
  void consumesUploadQuotaAtomicallyAndFailsWhenExhausted() {
    when(sessions.incrementUploads(ownerId, 3)).thenReturn(1, 0);

    assertDoesNotThrow(() -> service().consumeUpload(owner(OwnerKindEnum.KIND_ANONYMOUS)));
    TooManyRequestsException error = assertThrows(TooManyRequestsException.class,
        () -> service().consumeUpload(owner(OwnerKindEnum.KIND_ANONYMOUS)));
    assertEquals(ErrorCodes.Anonymous.UPLOAD_LIMIT_REACHED, error.getMessage());
  }

  @Test
  void extractionAndGenerationHaveTheirOwnLimits() {
    when(sessions.incrementExtractions(ownerId, 2)).thenReturn(0);
    when(sessions.incrementGenerations(ownerId, 1)).thenReturn(0);
    Owner anonymous = owner(OwnerKindEnum.KIND_ANONYMOUS);

    assertEquals(ErrorCodes.Anonymous.EXTRACTION_LIMIT_REACHED,
        assertThrows(TooManyRequestsException.class, () -> service().consumeExtraction(anonymous))
            .getMessage());
    assertEquals(ErrorCodes.Anonymous.GENERATION_LIMIT_REACHED,
        assertThrows(TooManyRequestsException.class, () -> service().consumeGeneration(anonymous))
            .getMessage());
  }

  @Test
  void accountOwnersAreNeverLimited() {
    Owner account = owner(OwnerKindEnum.KIND_ACCOUNT);

    assertDoesNotThrow(() -> {
      service().consumeUpload(account);
      service().consumeExtraction(account);
      service().consumeGeneration(account);
    });
    verifyNoInteractions(sessions);
  }

  private static byte[] pdf(int pages) throws Exception {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < pages; i++) {
        document.addPage(new PDPage());
      }
      document.save(out);
      return out.toByteArray();
    }
  }
}
