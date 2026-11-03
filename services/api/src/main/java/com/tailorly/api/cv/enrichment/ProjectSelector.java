package com.tailorly.api.cv.enrichment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvLanguage;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.shared.util.Text;

/**
 * Escolhe os projetos desta versão do currículo. Os projetos do Currículo Base continuam sendo a fonte;
 * projetos <b>candidatos</b> (repositórios do GitHub da pessoa que ela confirmou como evidência) entram
 * para completar o mínimo de {@value #MIN_PROJECTS} projetos e para trocar um projeto do Base que a vaga
 * pouco pede por um que ela pede mais. Um candidato só existe aqui se tem evidência confirmada, com fonte e trecho;
 * os detalhes dele vêm dos detalhes confirmados e passam pela mesma reescrita ancorada dos demais.
 * Nada é gravado no Ground Truth: é só a seleção desta geração, registrada em {@code tailoring}.
 */
public final class ProjectSelector {
  public static final int MIN_PROJECTS = 3;
  public static final int MAX_PROJECTS = 5;
  static final int MAX_DETAILS = 3;
  private static final String SECTION = "projects";

  private ProjectSelector() {
  }

  /** O documento com os projetos escolhidos, os nomes incluídos a partir de candidatos e os do Base deixados de fora. */
  public record Selection(CvDocument document, List<String> added, List<String> omitted) {
  }

  public static Selection apply(CvDocument document, List<ConfirmedEvidence> evidence, JobTerms terms,
      CvLanguage language) {
    List<CvDocument.Entry> base = projects(document);
    List<Candidate> candidates = candidates(evidence, terms);
    if (candidates.isEmpty()) {
      return new Selection(document, List.of(), List.of());
    }

    List<Scored> selected = new ArrayList<>();
    base.forEach(entry -> selected.add(new Scored(entry, relevance(entry, terms), false)));
    List<String> added = new ArrayList<>();
    List<String> omitted = new ArrayList<>();
    for (Candidate candidate : candidates) {
      Scored scored = new Scored(candidate.entry(), candidate.relevance(), true);
      if (selected.size() < MIN_PROJECTS || (selected.size() < MAX_PROJECTS && candidate.relevance() > 0)) {
        selected.add(scored);
        added.add(candidate.entry().title());
        continue;
      }
      // Cheio: o candidato só entra no lugar de um projeto do Base que a vaga pede menos que ele.
      Scored weakest = selected.stream().filter(item -> !item.candidate())
          .min(Comparator.comparingInt(Scored::relevance)).orElse(null);
      if (weakest != null && candidate.relevance() > weakest.relevance()) {
        selected.remove(weakest);
        selected.add(scored);
        omitted.add(weakest.entry().title());
        added.add(candidate.entry().title());
      }
    }
    if (added.isEmpty()) {
      return new Selection(document, List.of(), List.of());
    }

    // Ordenação estável: mais relevantes primeiro; empates mantêm a ordem do Base e depois a dos candidatos.
    List<CvDocument.Entry> ordered = selected.stream()
        .sorted(Comparator.comparingInt((Scored item) -> -item.relevance()))
        .map(Scored::entry).collect(Collectors.toList());
    return new Selection(withProjects(document, ordered, language), List.copyOf(added), List.copyOf(omitted));
  }

  private record Scored(CvDocument.Entry entry, int relevance, boolean candidate) {
  }

  private record Candidate(CvDocument.Entry entry, int relevance) {
  }

  /** Um candidato por projeto (chave {@code github:repo}), do mais relevante para a vaga ao menos. */
  private static List<Candidate> candidates(List<ConfirmedEvidence> evidence, JobTerms terms) {
    Map<String, List<ConfirmedEvidence>> byProject = new LinkedHashMap<>();
    evidence.stream().filter(ConfirmedEvidence::isCandidateProject)
        .forEach(item -> byProject.computeIfAbsent(item.subjectKey(), key -> new ArrayList<>()).add(item));
    List<Candidate> candidates = new ArrayList<>();
    byProject.forEach((key, items) -> {
      ConfirmedEvidence first = items.getFirst();
      String title = Text.normalize(first.subjectLabel()).isEmpty()
          ? key.substring(ConfirmedEvidence.CANDIDATE_PREFIX.length()) : first.subjectLabel().strip();
      List<String> details = items.stream().map(ConfirmedEvidence::claim)
          .filter(claim -> claim != null && !claim.isBlank()).limit(MAX_DETAILS).toList();
      CvDocument.Entry entry = new CvDocument.Entry(key, title, null, null, null, details, first.subjectUrl());
      candidates.add(new Candidate(entry, relevance(entry, items, terms)));
    });
    candidates.sort(Comparator.comparingInt((Candidate item) -> -item.relevance()));
    return candidates;
  }

  private static int relevance(CvDocument.Entry entry, JobTerms terms) {
    return terms.matchedIn(text(entry, List.of())).size();
  }

  private static int relevance(CvDocument.Entry entry, List<ConfirmedEvidence> evidence, JobTerms terms) {
    return terms.matchedIn(text(entry, evidence)).size();
  }

  private static String text(CvDocument.Entry entry, List<ConfirmedEvidence> evidence) {
    List<String> parts = new ArrayList<>();
    parts.add(entry.title());
    parts.add(entry.subtitle());
    parts.addAll(entry.details());
    evidence.forEach(item -> parts.add(item.excerpt()));
    return parts.stream().filter(part -> part != null && !part.isBlank()).collect(Collectors.joining("\n"));
  }

  private static List<CvDocument.Entry> projects(CvDocument document) {
    return document.sections().stream().filter(section -> SECTION.equals(section.key())).findFirst()
        .map(CvDocument.Section::entries).orElse(List.of());
  }

  private static CvDocument withProjects(CvDocument document, List<CvDocument.Entry> entries, CvLanguage language) {
    List<CvDocument.Section> sections = new ArrayList<>();
    boolean present = false;
    for (CvDocument.Section section : document.sections()) {
      if (SECTION.equals(section.key())) {
        sections.add(section.withEntries(List.copyOf(entries)));
        present = true;
      } else {
        sections.add(section);
      }
    }
    if (!present) {
      // Sem seção de projetos no Base: ela entra logo depois da experiência, onde costuma ficar.
      int at = 0;
      for (int i = 0; i < sections.size(); i++) {
        if ("experience".equals(sections.get(i).key())) {
          at = i + 1;
        }
      }
      sections.add(at, new CvDocument.Section(SECTION, language.heading(SECTION), false, List.copyOf(entries)));
    }
    return new CvDocument(document.name(), document.headline(), document.summary(), document.contacts(),
        List.copyOf(sections), document.layout());
  }
}
