package com.tailorly.api.cv.evidence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailorly.api.base.snapshot.GtSnapshotItem;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvDocumentBuilder;
import com.tailorly.api.cv.model.FactValue;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.shared.util.Text;

/**
 * Descobre, nos fatos aprovados de um snapshot, os itens que podem receber evidências. Monta o mesmo
 * documento que a geração monta, para que a chave de cada item seja o id da entrada correspondente.
 */
final class EvidenceSubjects {
  static final int MAX_SUBJECTS = 20;
  private static final int MIN_ALIAS = 3;
  private static final Pattern REPO = Pattern.compile("(?:github|gitlab)\\.com/[^/\\s]+/([\\w.-]+)", Pattern.CASE_INSENSITIVE);

  private EvidenceSubjects() {
  }

  static List<EvidenceSubject> of(List<GtSnapshotItem> items) {
    List<FactValue> facts = items.stream().map(item -> new FactValue(item.getCategory(),
        item.getFactRecord().getDisplayOrder(), item.getFieldKey(),
        item.getApprovedValue() instanceof String || item.getApprovedValue() instanceof Number
            || item.getApprovedValue() instanceof Boolean ? item.getApprovedValue().toString() : null)).toList();
    return of(CvDocumentBuilder.build(facts, JobTerms.of(null, null)).document());
  }

  static List<EvidenceSubject> of(CvDocument document) {
    List<EvidenceSubject> subjects = new ArrayList<>();
    for (CvDocument.Section section : document.sections()) {
      for (CvDocument.Entry entry : section.entries()) {
        if (subjects.size() == MAX_SUBJECTS || entry.id() == null) {
          continue;
        }
        switch (section.key()) {
          case "projects" -> subjects.add(project(entry));
          case "experience" -> experience(entry).ifPresent(subjects::add);
          default -> {
          }
        }
      }
    }
    return List.copyOf(subjects);
  }

  private static EvidenceSubject project(CvDocument.Entry entry) {
    Set<String> aliases = new LinkedHashSet<>();
    add(aliases, entry.title());
    String link = entry.link();
    if (link != null) {
      Matcher repo = REPO.matcher(link);
      if (repo.find()) {
        add(aliases, repo.group(1));
      }
    }
    // O contexto (empresa, curso) afina a busca sem virar apelido: ele não nomeia o projeto.
    String query = entry.subtitle() == null || entry.subtitle().length() > 60
        ? entry.title() : entry.title() + " " + entry.subtitle();
    return new EvidenceSubject(entry.id(), EvidenceSubject.Kind.PROJECT, entry.title(), List.copyOf(aliases),
        link, query);
  }

  /** Uma experiência é citada pela empresa; sem empresa não há nome seguro para procurar em notas. */
  private static java.util.Optional<EvidenceSubject> experience(CvDocument.Entry entry) {
    Set<String> aliases = new LinkedHashSet<>();
    add(aliases, entry.subtitle());
    if (aliases.isEmpty()) {
      return java.util.Optional.empty();
    }
    String label = entry.title() + " — " + entry.subtitle();
    return java.util.Optional.of(new EvidenceSubject(entry.id(), EvidenceSubject.Kind.EXPERIENCE, label,
        List.copyOf(aliases), null, null));
  }

  private static void add(Set<String> aliases, String name) {
    String value = Text.normalize(name);
    if (value.length() >= MIN_ALIAS) {
      aliases.add(value);
    }
  }
}
