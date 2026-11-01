package com.tailorly.api.cv.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GenerationProgressTest {
  private final GenerationProgress progress = new GenerationProgress();

  @Test
  void followsTheStepsOfTheRunAndKeepsTheFinalState() {
    UUID owner = UUID.randomUUID();
    assertTrue(progress.current(owner).isEmpty());

    var tracker = progress.begin(owner);
    assertEquals(GenerationProgress.PREPARING, progress.current(owner).orElseThrow().stage());

    tracker.step(GenerationProgress.REFINING, "ajuste", 2);
    var refining = progress.current(owner).orElseThrow();
    assertEquals(GenerationProgress.RUNNING, refining.status());
    assertEquals(GenerationProgress.REFINING, refining.stage());
    assertEquals(2, refining.round());

    progress.finish(owner);
    var done = progress.current(owner).orElseThrow();
    assertEquals(GenerationProgress.DONE, done.status());
    assertEquals(GenerationProgress.REFINING, done.stage());
  }

  @Test
  void progressOfOneOwnerDoesNotLeakToAnother() {
    UUID first = UUID.randomUUID();
    progress.begin(first);
    assertTrue(progress.current(UUID.randomUUID()).isEmpty());
  }
}
