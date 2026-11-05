package com.tailorly.api.job.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.tailorly.api.job.dto.JobPostingResponse;
import org.junit.jupiter.api.Test;

class RequirementCandidateTest {

  private static List<RequirementCandidate> candidates(String required, String preferred) {
    String source = required + "\n" + preferred;
    var job = JobPostingResponse.builder()
        .requirements(List.of(required))
        .niceToHave(List.of(preferred))
        .build();
    return RequirementCandidate.from(job, source);
  }

  @Test
  void plusInsideARequirementDoesNotMakeItDesirable() {
    var result = candidates("Plus de 3 anos de Java", "Conhecimento em Kafka");
    assertEquals("required", result.get(0).importance());
    assertEquals("preferred", result.get(1).importance());
  }

  @Test
  void desirableWordingInTheTextStillMarksPreferred() {
    var result = candidates("Experiência com Go é um diferencial", "Kafka");
    assertEquals("preferred", result.get(0).importance());
  }
}
