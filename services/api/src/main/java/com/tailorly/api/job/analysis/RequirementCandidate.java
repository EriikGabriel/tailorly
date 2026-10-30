package com.tailorly.api.job.analysis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.tailorly.api.job.dto.JobPostingResponse;

/** Requisito já isolado pela divisão por títulos, com a importância da seção em que estava. */
record RequirementCandidate(String text, String importance) {
  static final int MAX_REQUIREMENTS = 60;

  private static final Pattern DESIRABLE_WORDING =
      Pattern.compile("desejav|diferencia|nice to have|bonus|\\bplus\\b|preferenc");

  /**
   * Requisitos e desejáveis que a divisão por títulos já encontrou, na ordem do texto. Só vale o que
   * é trecho literal da vaga, para a evidência nunca ser inventada.
   */
  static List<RequirementCandidate> from(JobPostingResponse job, String source) {
    List<RequirementCandidate> result = new ArrayList<>();
    addCandidates(result, job.requirements(), "required", source);
    addCandidates(result, job.niceToHave(), "preferred", source);
    return result.size() > MAX_REQUIREMENTS ? result.subList(0, MAX_REQUIREMENTS) : result;
  }

  private static void addCandidates(List<RequirementCandidate> target, List<String> items, String importance, String source) {
    if (items == null)
      return;
    for (String item : items) {
      String text = item == null ? "" : item.strip();
      if (text.isEmpty() || !source.contains(text))
        continue;
      String lower = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
          .replaceAll("\\p{M}+", "");
      // Um rótulo no próprio texto ("diferencial", "desejável") vale mais que a seção em que caiu.
      target.add(new RequirementCandidate(text, DESIRABLE_WORDING.matcher(lower).find() ? "preferred" : importance));
    }
  }
}
