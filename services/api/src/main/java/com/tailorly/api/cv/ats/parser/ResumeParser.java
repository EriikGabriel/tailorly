package com.tailorly.api.cv.ats.parser;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Lê um PDF de currículo do jeito que um parser de triagem lê, sem conhecer o que foi escrito nele:
 * extrai o texto, adivinha o nome, acha contatos por padrão, reconhece seções só pelos títulos
 * padrão (em português e inglês) e separa entradas pelas linhas que trazem um período. Um título
 * criativo ("Minha trajetória") não é reconhecido, e é exatamente isso que a avaliação precisa
 * enxergar. Determinístico e local: nada sai do servidor.
 */
@Component
public class ResumeParser {
  private static final Map<String, String> HEADINGS = Map.ofEntries(
      Map.entry("resumo", "summary"), Map.entry("resumo profissional", "summary"), Map.entry("sobre", "summary"),
      Map.entry("perfil", "summary"), Map.entry("summary", "summary"), Map.entry("profile", "summary"),
      Map.entry("experiencia", "experience"), Map.entry("experiencia profissional", "experience"),
      Map.entry("experiencias profissionais", "experience"), Map.entry("work experience", "experience"),
      Map.entry("experience", "experience"), Map.entry("professional experience", "experience"),
      Map.entry("formacao", "education"), Map.entry("formacao academica", "education"),
      Map.entry("educacao", "education"), Map.entry("education", "education"),
      Map.entry("projetos", "projects"), Map.entry("projects", "projects"),
      Map.entry("tecnologias", "skills"), Map.entry("habilidades", "skills"), Map.entry("habilidades tecnicas", "skills"),
      Map.entry("competencias", "skills"), Map.entry("skills", "skills"), Map.entry("technical skills", "skills"),
      Map.entry("certificacoes", "certifications"), Map.entry("certificados", "certifications"),
      Map.entry("certifications", "certifications"),
      Map.entry("idiomas", "languages"), Map.entry("languages", "languages"),
      Map.entry("voluntariado", "volunteering"), Map.entry("volunteering", "volunteering"));
  private static final Set<String> DATED = Set.of("experience", "education", "volunteering");

  private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
  private static final Pattern PHONE = Pattern.compile("(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2}\\)?[\\s.-]?)\\d{4,5}[\\s.-]?\\d{4}");
  private static final Pattern LINK = Pattern.compile(
      "(?:https?://)?(?:www\\.)?(?:linkedin\\.com|github\\.com|gitlab\\.com)/[\\w./%-]+|(?:https?://)[\\w.-]+\\.[a-z]{2,}[\\w./%?=&#-]*",
      Pattern.CASE_INSENSITIVE);
  private static final String MONTH = "(?:\\d{1,2}/)?\\d{4}";
  private static final Pattern PERIOD = Pattern.compile(
      "(" + MONTH + ")\\s*[–—-]\\s*(" + MONTH + "|atual|presente|em andamento|present|current)\\b|\\b((?:19|20)\\d{2})\\b",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern BULLET = Pattern.compile("^\\s*[•·▪◦*–-]\\s+");

  /** {@code empty} quando o PDF não abre ou não tem texto: a avaliação trata isso como falha de leitura. */
  public Optional<ParsedResume> parse(byte[] pdf) {
    if (pdf == null) {
      return Optional.empty();
    }
    try (PDDocument document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);
      return text.isBlank() ? Optional.empty() : Optional.of(parseText(text, document.getNumberOfPages()));
    } catch (IOException | RuntimeException exception) {
      return Optional.empty();
    }
  }

  ParsedResume parseText(String text, int pages) {
    List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();

    List<ParsedResume.Section> sections = new ArrayList<>();
    List<String> header = new ArrayList<>();
    String heading = null;
    String key = null;
    List<String> current = new ArrayList<>();
    for (String line : lines) {
      Optional<String> canonical = canonicalHeading(line);
      if (canonical.isPresent()) {
        flush(sections, heading, key, current);
        heading = line;
        key = canonical.get();
        current = new ArrayList<>();
      } else if (heading == null) {
        header.add(line);
      } else {
        current.add(line);
      }
    }
    flush(sections, heading, key, current);

    String headerText = String.join("\n", header);
    List<String> emails = all(EMAIL, text);
    List<String> phones = all(PHONE, headerText.isBlank() ? text : headerText);
    List<String> links = all(LINK, text);
    return new ParsedResume(text, pages, guessName(header), emails, phones, links, List.copyOf(sections),
        skills(sections));
  }

  private static Optional<String> canonicalHeading(String line) {
    String folded = fold(line).replaceAll("[:.]+$", "").strip();
    return folded.length() > 40 ? Optional.empty() : Optional.ofNullable(HEADINGS.get(folded));
  }

  private static void flush(List<ParsedResume.Section> sections, String heading, String key, List<String> lines) {
    if (heading != null) {
      sections.add(new ParsedResume.Section(heading, key, List.copyOf(lines),
          key != null && DATED.contains(key) ? entries(lines) : List.of()));
    }
  }

  /**
   * Cada linha com período abre uma entrada; as seguintes, até a próxima, são o corpo dela. Quando o
   * período vem sozinho na linha (template com a data embaixo do cargo), o cargo são as duas linhas
   * de texto anteriores.
   */
  private static List<ParsedResume.Entry> entries(List<String> lines) {
    List<ParsedResume.Entry> entries = new ArrayList<>();
    String head = null;
    String period = null;
    List<String> body = new ArrayList<>();
    List<String> recent = new ArrayList<>();
    for (String line : lines) {
      Matcher match = PERIOD.matcher(line);
      boolean bullet = BULLET.matcher(line).find();
      boolean dated = match.find() && !bullet;
      String before = dated ? line.substring(0, match.start()).strip() : "";
      if (dated && (before.length() >= 3 || (before.isEmpty() && !recent.isEmpty()))) {
        if (head != null) {
          entries.add(new ParsedResume.Entry(head, period, List.copyOf(body)));
        }
        head = before.isEmpty() ? String.join(" ", recent) : before;
        if (before.isEmpty() && head != null) {
          // As linhas do cargo não fazem parte do corpo da entrada anterior.
          trim(entries, recent.size());
        }
        period = line.substring(match.start()).strip();
        body = new ArrayList<>();
        recent.clear();
      } else {
        String content = BULLET.matcher(line).replaceFirst("");
        if (head != null) {
          body.add(content);
        }
        if (!bullet) {
          recent.add(line);
          if (recent.size() > 2) {
            recent.remove(0);
          }
        }
      }
    }
    if (head != null) {
      entries.add(new ParsedResume.Entry(head, period, List.copyOf(body)));
    }
    return entries;
  }

  /** Tira do fim do corpo da última entrada as linhas que, na verdade, eram o cargo da próxima. */
  private static void trim(List<ParsedResume.Entry> entries, int count) {
    if (entries.isEmpty()) {
      return;
    }
    ParsedResume.Entry last = entries.removeLast();
    List<String> body = new ArrayList<>(last.body());
    for (int i = 0; i < count && !body.isEmpty(); i++) {
      body.removeLast();
    }
    entries.add(new ParsedResume.Entry(last.head(), last.period(), List.copyOf(body)));
  }

  /** O nome provável: a primeira linha do topo que não parece contato nem título profissional longo. */
  private static String guessName(List<String> header) {
    return header.stream()
        .filter(line -> !EMAIL.matcher(line).find() && !LINK.matcher(line).find()
            && line.chars().filter(Character::isDigit).count() < 4 && line.length() <= 60)
        .findFirst().orElse(null);
  }

  private static Set<String> skills(List<ParsedResume.Section> sections) {
    Set<String> skills = new LinkedHashSet<>();
    for (ParsedResume.Section section : sections) {
      if (!"skills".equals(section.key())) {
        continue;
      }
      for (String line : section.lines()) {
        String content = line.contains(":") ? line.substring(line.indexOf(':') + 1) : line;
        for (String token : BULLET.matcher(content).replaceFirst("").split("[,;·|•]")) {
          String skill = token.strip().replaceAll("[.]+$", "");
          if (!skill.isEmpty() && skill.length() <= 40) {
            skills.add(skill);
          }
        }
      }
    }
    return skills;
  }

  private static List<String> all(Pattern pattern, String text) {
    List<String> found = new ArrayList<>();
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      found.add(matcher.group().strip());
    }
    return List.copyOf(new LinkedHashSet<>(found));
  }

  public static String fold(String text) {
    return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
        .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).strip();
  }
}
