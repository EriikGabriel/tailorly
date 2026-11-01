package com.tailorly.api.base.version;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BaseVersionStatusTest {
  @Test
  void activeVersionsCanBeCorrectedAndApprovedAgain() {
    assertTrue(BaseVersionStatus.isReviewable("CANDIDATE"));
    assertTrue(BaseVersionStatus.isReviewable("EXTRACTED"));
    assertTrue(BaseVersionStatus.isReviewable("ACTIVE"));
  }

  @Test
  void supersededAndDeactivatedVersionsStayClosed() {
    assertFalse(BaseVersionStatus.isReviewable("SUPERSEDED"));
    assertFalse(BaseVersionStatus.isReviewable("DEACTIVATED"));
    assertFalse(BaseVersionStatus.isReviewable(null));
  }
}
