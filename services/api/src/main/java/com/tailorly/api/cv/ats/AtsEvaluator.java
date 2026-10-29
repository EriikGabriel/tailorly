package com.tailorly.api.cv.ats;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.JobTerms;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Pontua o currículo como um ATS simples o leria: extrai o texto do PDF compilado (a mesma operação
 * que um parser de triagem faz) e confere se nome, contato, seções e termos da vaga sobrevivem, na
 * ordem certa. Determinístico e sem IA, então serve de critério objetivo para a esteira comparar
 * versões do layout.
 */
@Component
public class AtsEvaluator {
  public static final int TARGET_SCORE = 90;

  public AtsReport evaluate(CvDocument document, JobTerms terms, byte[] pdf) {
    ExtractedPdf extracted = extract(pdf);
    // O que conta é o texto que um parser enxerga; o do documento é só o fallback quando o PDF não abre.
    String text = extracted.text() != null ? extracted.text() : flatten(document);
    String folded = fold(text);

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
        ? new AtsReport.AtsCheck("keywords", "Termos da vaga no currículo", 40, 40,
            "A vaga não traz termos técnicos para cruzar")
        : new AtsReport.AtsCheck("keywords", "Termos da vaga no currículo", Math.round(40f * found / total), 40,
            matched.size() + " de " + terms.terms().size() + " termos aparecem no texto"));

    String email = document.contacts().stream()
        .filter(contact -> contact.url() != null && contact.url().startsWith("mailto:"))
        .map(CvDocument.Contact::text).findFirst().orElse(null);
    boolean hasName = document.name() != null;
    boolean hasEmail = email != null;
    boolean hasPhone = document.contacts().stream()
        .anyMatch(contact -> contact.text().replaceAll("\\D", "").length() >= 8);
    checks.add(new AtsReport.AtsCheck("contact", "Nome e contato",
        (hasName ? 3 : 0) + (hasEmail ? 4 : 0) + (hasPhone ? 3 : 0), 10,
        "Nome " + status(hasName) + ", e-mail " + status(hasEmail) + ", telefone " + status(hasPhone)));

    boolean work = has(document, "experience") || has(document, "projects");
    boolean education = has(document, "degrees");
    boolean skills = has(document, "technologies");
    checks.add(new AtsReport.AtsCheck("sections", "Seções esperadas",
        (work ? 5 : 0) + (education ? 5 : 0) + (skills ? 5 : 0), 15,
        "Experiência ou projetos " + status(work) + ", formação " + status(education)
            + ", tecnologias " + status(skills)));

    int parse = 0;
    String parseDetail;
    if (extracted.text() == null) {
      parseDetail = "Não foi possível ler o texto do PDF";
    } else {
      boolean readable = extracted.text().strip().length() > 50 && !extracted.text().contains("�");
      boolean nameOk = hasName && folded.contains(fold(document.name()));
      boolean emailOk = !hasEmail || folded.contains(fold(email));
      boolean orderOk = headingsInOrder(document, folded);
      parse = (readable ? 8 : 0) + (nameOk ? 4 : 0) + (emailOk ? 4 : 0) + (orderOk ? 4 : 0);
      parseDetail = "Texto legível " + status(readable) + ", nome " + status(nameOk) + ", e-mail "
          + status(emailOk) + ", seções na ordem " + status(orderOk);
    }
    checks.add(new AtsReport.AtsCheck("parsing", "Leitura do texto do PDF", parse, 20, parseDetail));

    int words = text.isBlank() ? 0 : text.strip().split("\\s+").length;
    boolean pagesOk = extracted.pages() != null && extracted.pages() <= 2;
    boolean wordsOk = words >= 250 && words <= 1000;
    checks.add(new AtsReport.AtsCheck("length", "Tamanho", (pagesOk ? 5 : 0) + (wordsOk ? 5 : 0), 10,
        (extracted.pages() == null ? "?" : extracted.pages()) + " página(s), " + words + " palavras"));

    List<CvDocument.Entry> jobs = document.sections().stream()
        .filter(section -> "experience".equals(section.key()))
        .flatMap(section -> section.entries().stream()).toList();
    long dated = jobs.stream().filter(entry -> entry.period() != null).count();
    checks.add(new AtsReport.AtsCheck("dates", "Datas nas experiências",
        jobs.isEmpty() ? 5 : Math.round(5f * dated / jobs.size()), 5,
        jobs.isEmpty() ? "Sem experiências" : dated + " de " + jobs.size() + " com período"));

    int score = checks.stream().mapToInt(AtsReport.AtsCheck::points).sum();
    return new AtsReport(score, List.copyOf(checks), List.copyOf(matched), List.copyOf(missing),
        extracted.pages(), words);
  }

  private record ExtractedPdf(String text, Integer pages) {
  }

  private static ExtractedPdf extract(byte[] pdf) {
    if (pdf == null) {
      return new ExtractedPdf(null, null);
    }
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return new ExtractedPdf(new PDFTextStripper().getText(document), document.getNumberOfPages());
    } catch (IOException | RuntimeException exception) {
      return new ExtractedPdf(null, null);
    }
  }

  private static boolean headingsInOrder(CvDocument document, String foldedText) {
    int from = 0;
    for (CvDocument.Section section : document.sections()) {
      int at = foldedText.indexOf(fold(section.heading()), from);
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

  private static String fold(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
        .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }
}
