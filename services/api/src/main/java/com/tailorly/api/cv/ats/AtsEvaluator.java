package com.tailorly.api.cv.ats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.cv.ats.parser.ParsedResume;
import com.tailorly.api.cv.ats.parser.ResumeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pontua o currículo como um ATS simples o leria: lê o PDF compilado com {@link ResumeParser} (sem
 * conhecer o que foi escrito nele) e confere se nome, contato, seções, cargos com data, tecnologias e
 * termos da vaga sobrevivem à leitura, na ordem certa. Determinístico e sem IA, então serve de critério objetivo para a esteira comparar
 * versões do layout.
 */
@Component
@RequiredArgsConstructor
public class AtsEvaluator {
  public static final int TARGET_SCORE = 90;

  private final ResumeParser parser;

  public AtsReport evaluate(CvDocument document, JobTerms terms, byte[] pdf) {
    Optional<ParsedResume> parsed = parser.parse(pdf);
    // O que conta é o texto que um parser enxerga; o do documento é só o fallback quando o PDF não abre.
    String text = parsed.map(ParsedResume::text).orElseGet(() -> flatten(document));
    String folded = ResumeParser.fold(text);

    List<AtsReport.AtsCheck> checks = new ArrayList<>();
    List<String> matched = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    // Cobertura ponderada dos termos da vaga (obrigatórios pesam o dobro).
    Set<String> hits = new HashSet<>(terms.matchedIn(text));
    int total = 0;
    int found = 0;
    for (String term : terms.terms()) {
      int weight = terms.weight(term);
      total += weight;
      if (hits.contains(term)) {
        found += weight;
        matched.add(term);
      } else {
        missing.add(term);
      }
    }
    checks.add(total == 0
        ? new AtsReport.AtsCheck("keywords", "Termos da vaga no currículo", 35, 35,
            "A vaga não traz termos técnicos para cruzar")
        : new AtsReport.AtsCheck("keywords", "Termos da vaga no currículo", Math.round(35f * found / total), 35,
            matched.size() + " de " + terms.terms().size() + " termos aparecem no texto"));

    checks.add(contactCheck(document, parsed.orElse(null)));
    checks.add(sectionsCheck(document, parsed.orElse(null)));
    checks.add(parsingCheck(document, parsed.orElse(null), folded));

    int words = text.isBlank() ? 0 : text.strip().split("\\s+").length;
    Integer pages = parsed.map(ParsedResume::pages).orElse(null);
    boolean pagesOk = pages != null && pages <= 2;
    boolean wordsOk = words >= 250 && words <= 1000;
    checks.add(new AtsReport.AtsCheck("length", "Tamanho", (pagesOk ? 5 : 0) + (wordsOk ? 5 : 0), 10,
        (pages == null ? "?" : pages) + " página(s), " + words + " palavras"));

    List<CvDocument.Entry> jobs = entriesOf(document, "experience");
    long dated = jobs.stream().filter(entry -> entry.period() != null).count();
    checks.add(new AtsReport.AtsCheck("dates", "Datas nas experiências",
        jobs.isEmpty() ? 5 : Math.round(5f * dated / jobs.size()), 5,
        jobs.isEmpty() ? "Sem experiências" : dated + " de " + jobs.size() + " com período"));

    int score = checks.stream().mapToInt(AtsReport.AtsCheck::points).sum();
    return new AtsReport(score, List.copyOf(checks), List.copyOf(matched), List.copyOf(missing), pages, words);
  }

  /** Nome, e-mail e telefone como o parser os acharia no topo do PDF, não como foram escritos no documento. */
  private static AtsReport.AtsCheck contactCheck(CvDocument document, ParsedResume parsed) {
    String email = document.contacts().stream()
        .filter(contact -> contact.url() != null && contact.url().startsWith("mailto:"))
        .map(CvDocument.Contact::text).findFirst().orElse(null);
    boolean hasPhone = document.contacts().stream()
        .anyMatch(contact -> contact.text().replaceAll("\\D", "").length() >= 8);
    boolean name;
    boolean mail;
    boolean phone;
    if (parsed == null) {
      name = document.name() != null;
      mail = email != null;
      phone = hasPhone;
    } else {
      name = document.name() != null && parsed.name() != null
          && ResumeParser.fold(parsed.name()).contains(ResumeParser.fold(document.name()));
      mail = email != null && parsed.emails().stream().anyMatch(found -> found.equalsIgnoreCase(email));
      phone = hasPhone && !parsed.phones().isEmpty();
    }
    return new AtsReport.AtsCheck("contact", "Nome e contato",
        (name ? 3 : 0) + (mail ? 4 : 0) + (phone ? 3 : 0), 10,
        "Nome " + status(name) + ", e-mail " + status(mail) + ", telefone " + status(phone)
            + (parsed == null ? "" : " (como o parser leu)"));
  }

  /** As seções esperadas precisam existir com título que um parser reconhece, não só no documento. */
  private static AtsReport.AtsCheck sectionsCheck(CvDocument document, ParsedResume parsed) {
    boolean work = has(document, "experience") || has(document, "projects");
    boolean education = has(document, "degrees");
    boolean skills = has(document, "technologies");
    if (parsed != null) {
      work = work && (parsed.hasSection("experience") || parsed.hasSection("projects"));
      education = education && parsed.hasSection("education");
      skills = skills && parsed.hasSection("skills");
    }
    return new AtsReport.AtsCheck("sections", "Seções esperadas",
        (work ? 5 : 0) + (education ? 5 : 0) + (skills ? 5 : 0), 15,
        "Experiência ou projetos " + status(work) + ", formação " + status(education)
            + ", tecnologias " + status(skills));
  }

  /** Quanto do conteúdo escrito um parser recupera: cargos com datas, formação, tecnologias e ordem. */
  private static AtsReport.AtsCheck parsingCheck(CvDocument document, ParsedResume parsed, String folded) {
    if (parsed == null) {
      return new AtsReport.AtsCheck("parsing", "Leitura do texto do PDF", 0, 25,
          "Não foi possível ler o texto do PDF");
    }
    boolean readable = parsed.text().strip().length() > 50 && !parsed.text().contains("�");
    boolean orderOk = headingsInOrder(document, folded);
    float experience = recovered(entriesOf(document, "experience"), parsed.entriesOf("experience"));
    float education = recovered(entriesOf(document, "degrees"), parsed.entriesOf("education"));
    float skills = skillsRecovered(document, parsed);
    int points = (readable ? 4 : 0) + Math.round(8 * experience) + Math.round(4 * education)
        + Math.round(5 * skills) + (orderOk ? 4 : 0);
    return new AtsReport.AtsCheck("parsing", "Leitura do texto do PDF", points, 25,
        "Texto legível " + status(readable) + ", cargos com data " + percent(experience) + ", formação "
            + percent(education) + ", tecnologias " + percent(skills) + ", seções na ordem " + status(orderOk));
  }

  /** Fração das entradas escritas que o parser recupera com o título e, quando há, o período. */
  private static float recovered(List<CvDocument.Entry> written, List<ParsedResume.Entry> read) {
    if (written.isEmpty()) {
      return 1f;
    }
    long ok = written.stream().filter(entry -> read.stream().anyMatch(candidate ->
        ResumeParser.fold(candidate.head()).contains(ResumeParser.fold(entry.title()))
            && (entry.period() == null || candidate.period() != null))).count();
    return (float) ok / written.size();
  }

  private static float skillsRecovered(CvDocument document, ParsedResume parsed) {
    List<String> written = new ArrayList<>();
    for (CvDocument.Section section : document.sections()) {
      if (!"technologies".equals(section.key())) {
        continue;
      }
      for (CvDocument.Entry entry : section.entries()) {
        if (section.grouped()) {
          entry.detailLines().forEach(line -> Arrays.stream(line.split(",")).map(String::strip)
              .filter(item -> !item.isEmpty()).forEach(written::add));
        } else if (entry.title() != null) {
          written.add(entry.title());
        }
      }
    }
    if (written.isEmpty()) {
      return 1f;
    }
    Set<String> read = new HashSet<>();
    parsed.skills().forEach(skill -> read.add(ResumeParser.fold(skill)));
    long ok = written.stream().filter(item -> {
      String folded = ResumeParser.fold(item);
      return read.stream().anyMatch(skill -> skill.equals(folded) || skill.startsWith(folded + " ("));
    }).count();
    return (float) ok / written.size();
  }

  private static List<CvDocument.Entry> entriesOf(CvDocument document, String key) {
    return document.sections().stream().filter(section -> section.key().equals(key))
        .flatMap(section -> section.entries().stream()).toList();
  }

  private static String percent(float ratio) {
    return Math.round(ratio * 100) + "%";
  }

  private static boolean headingsInOrder(CvDocument document, String foldedText) {
    int from = 0;
    for (CvDocument.Section section : document.sections()) {
      int at = foldedText.indexOf(ResumeParser.fold(section.heading()), from);
      if (at < 0) {
        return false;
      }
      from = at;
    }
    return true;
  }

  private static boolean has(CvDocument document, String key) {
    return document.sections().stream().anyMatch(section -> section.key().equals(key));
  }

  private static String status(boolean value) {
    return value ? "ok" : "ausente";
  }

  /** Todo o texto visível do documento, na ordem de leitura. */
  public static String flatten(CvDocument document) {
    StringBuilder out = new StringBuilder();
    append(out, document.name());
    append(out, document.headline());
    append(out, document.summary());
    document.contacts().forEach(contact -> append(out, contact.text()));
    for (CvDocument.Section section : document.sections()) {
      append(out, section.heading());
      for (CvDocument.Entry entry : section.entries()) {
        append(out, entry.title());
        append(out, entry.subtitle());
        append(out, entry.meta());
        append(out, entry.period());
        entry.details().forEach(detail -> append(out, detail));
      }
    }
    return out.toString();
  }

  private static void append(StringBuilder out, String text) {
    if (text != null) {
      out.append(text).append('\n');
    }
  }

}
