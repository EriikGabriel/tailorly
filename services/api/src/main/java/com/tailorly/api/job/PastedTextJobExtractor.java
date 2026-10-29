package com.tailorly.api.job;

import static com.tailorly.api.job.JobValues.blankToNull;

import java.util.List;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingResponse.Company;
import org.springframework.stereotype.Component;

/** Monta uma vaga a partir de texto colado pela pessoa, sem acessar nenhuma página. */
@Component
public class PastedTextJobExtractor {
  private static final int MAX_TITLE = 120;

  /** O texto colado é sempre texto puro: um {@code <} digitado não é interpretado como HTML. */
  public JobPostingResponse extract(String text, String title, String companyName) {
    JobContentBuilder content = new JobContentBuilder();
    List<HtmlText.Line> lines = HtmlText.plainLines(text);
    content.addLines(lines, JobSection.OTHER);

    String resolvedTitle = blankToNull(title);
    // Sem título informado, a primeira linha só vale se parecer um título (curta, sem pontuação final).
    if (resolvedTitle == null && !lines.isEmpty()) {
      String first = lines.getFirst().text();
      boolean looksLikeTitle = first.length() <= MAX_TITLE && !first.matches(".*[.:;!?]$")
          // "Requisitos e qualificações" é um cabeçalho de seção, não o nome da vaga.
          && JobSectionClassifier.sectionFor(first) == null;
      if (looksLikeTitle)
        resolvedTitle = first;
    }

    String company = blankToNull(companyName);
    return content.applyTo(JobPostingResponse.builder())
        .platform("text")
        .title(resolvedTitle)
        .company(company == null ? null : new Company(company, null, null, null, null, null))
        .warnings(List.of())
        .build();
  }
}
