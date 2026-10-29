package com.tailorly.api.job;

import java.util.ArrayList;
import java.util.List;

import com.tailorly.api.job.dto.JobPostingResponse;

/** Acumula blocos de descrição de uma vaga, classificando suas linhas por seção. */
final class JobContentBuilder {
  private final JobSections sections = new JobSections();
  private final List<String> blocks = new ArrayList<>();

  /** {@code defaultSection} vale até o primeiro título dentro do bloco. */
  void addPart(String heading, String markup, JobSection defaultSection) {
    List<HtmlText.Line> lines = HtmlText.fromMarkupOrText(markup);
    if (lines.isEmpty())
      return;
    JobSectionClassifier.classify(lines, defaultSection, sections);
    String body = HtmlText.join(lines);
    blocks.add(heading == null ? body : heading + "\n" + body);
  }

  void addLines(List<HtmlText.Line> lines, JobSection defaultSection) {
    if (lines.isEmpty())
      return;
    JobSectionClassifier.classify(lines, defaultSection, sections);
    blocks.add(HtmlText.join(lines));
  }

  void addItem(JobSection section, String text) {
    if (text != null && !text.isBlank())
      sections.add(section, text.trim());
  }

  JobSections sections() {
    return sections;
  }

  boolean isEmpty() {
    return blocks.isEmpty();
  }

  JobPostingResponse.JobPostingResponseBuilder applyTo(JobPostingResponse.JobPostingResponseBuilder builder) {
    return builder
        .descriptionText(String.join("\n\n", blocks))
        .responsibilities(sections.get(JobSection.RESPONSIBILITIES))
        .requirements(sections.get(JobSection.REQUIREMENTS))
        .niceToHave(sections.get(JobSection.NICE_TO_HAVE))
        .benefits(sections.get(JobSection.BENEFITS))
        .hiringProcess(sections.get(JobSection.HIRING_PROCESS));
  }
}
