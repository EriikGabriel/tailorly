package com.tailorly.api.job.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobAnalysis.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converte a resposta JSON do modelo em {@link JobAnalysis}. Só vale o que a vaga sustenta:
 * requisitos precisam de trecho literal, tecnologias precisam aparecer no texto e valores fora dos
 * conjuntos permitidos são descartados.
 */
final class JobAnalysisParser {
  private static final Logger log = LoggerFactory.getLogger(JobAnalysisParser.class);
  private static final int MAX_TEXT = 400;
  private static final Set<String> SENIORITY = Set.of("intern", "junior", "mid", "senior", "staff", "lead");
  private static final Set<String> IMPORTANCE = Set.of("required", "preferred");
  private static final Set<String> CATEGORY = Set.of(
      "technology", "experience", "education", "language", "certification", "soft_skill", "other");

  private final ObjectMapper objectMapper;

  JobAnalysisParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Resposta do modo "anotar": os itens já vieram da divisão por títulos; o modelo só os classifica. */
  JobAnalysis parseAnnotated(String response, List<RequirementCandidate> candidates, String source, String title) {
    try {
      JsonNode root = objectMapper.readTree(AiChatClient.stripCodeFence(response));
      JsonNode items = root.path("items");
      if (!items.isArray() || items.isEmpty())
        throw new IllegalStateException("AI response must contain an items array");

      Map<Integer, JsonNode> byId = new HashMap<>();
      for (JsonNode item : items) {
        if (item.path("id").canConvertToInt())
          byId.put(item.path("id").asInt(), item);
      }

      String haystack = title == null ? source : title + "\n" + source;
      Set<String> allTechnologies = new LinkedHashSet<>();
      List<Requirement> requirements = new ArrayList<>();

      for (int i = 0; i < candidates.size(); i++) {
        RequirementCandidate candidate = candidates.get(i);
        // Item sem anotação continua na lista: a cobertura vem da divisão do texto, não do modelo.
        JsonNode note = byId.getOrDefault(i + 1, MissingNode.getInstance());
        if (note.path("keep").isBoolean() && !note.path("keep").asBoolean())
          continue;

        List<String> technologies = technologies(note.path("technologies"), candidate.text());
        allTechnologies.addAll(technologies);
        String category = enumValue(note.path("category"), CATEGORY);
        requirements.add(new Requirement(
            displayText(candidate.text()),
            candidate.importance(),
            category != null ? category : technologies.isEmpty() ? "other" : "technology",
            yearsOfExperience(note.path("minYears"), candidate.text()),
            technologies,
            candidate.text()));
      }

      allTechnologies.addAll(technologies(root.path("technologies"), haystack));
      String seniority = enumValue(root.path("seniority"), SENIORITY);
      String seniorityEvidence = seniority == null ? null : verbatim(haystack, text(root, "seniorityEvidence"));
      if (seniorityEvidence == null)
        seniority = null;

      log.info("Job analysis annotated {} of {} items", byId.size(), candidates.size());
      return JobAnalysis.builder()
          .seniority(seniority)
          .seniorityEvidence(seniorityEvidence)
          .requirements(List.copyOf(requirements))
          .technologies(List.copyOf(allTechnologies))
          .analyzerVersion(JobRequirementsAnalyzer.VERSION)
          .build();
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not parse job analysis response", exception);
    }
  }

  /** Texto de exibição: sem a pontuação de lista que fecha a linha na vaga. */
  private static String displayText(String text) {
    String trimmed = text.replaceAll("[\\s;,]+$", "");
    return trimmed.length() > MAX_TEXT ? trimmed.substring(0, MAX_TEXT) : trimmed;
  }

  /** Anos de experiência só valem quando o item fala em anos; "Java 11+" é versão, não experiência. */
  private static Integer yearsOfExperience(JsonNode node, String itemText) {
    if (!node.canConvertToInt())
      return null;
    String lower = itemText.toLowerCase(Locale.ROOT);
    boolean mentionsYears = lower.contains("ano") || lower.contains("year");
    int years = node.asInt();
    return mentionsYears && years > 0 && years < 60 ? years : null;
  }

  JobAnalysis parseFullList(String response, String source, String title) {
    try {
      JsonNode root = objectMapper.readTree(AiChatClient.stripCodeFence(response));
      JsonNode items = root.path("requirements");
      if (!items.isArray())
        throw new IllegalStateException("AI response must contain a requirements array");

      // O título também é evidência válida de senioridade e de tecnologia.
      String haystack = title == null ? source : title + "\n" + source;

      List<Requirement> requirements = new ArrayList<>();
      for (JsonNode item : items) {
        Requirement requirement = toRequirement(item, haystack);
        if (requirement != null && requirements.size() < RequirementCandidate.MAX_REQUIREMENTS)
          requirements.add(requirement);
      }

      int discarded = items.size() - requirements.size();
      if (discarded > 0)
        log.warn("Job analysis discarded {} of {} requirements without verifiable evidence", discarded, items.size());
      if (!items.isEmpty() && requirements.isEmpty())
        throw new IllegalStateException("AI response contained no requirement with verifiable evidence");

      String seniority = enumValue(root.path("seniority"), SENIORITY);
      String seniorityEvidence = seniority == null ? null : verbatim(haystack, text(root, "seniorityEvidence"));
      if (seniorityEvidence == null)
        seniority = null;

      return JobAnalysis.builder()
          .seniority(seniority)
          .seniorityEvidence(seniorityEvidence)
          .requirements(List.copyOf(requirements))
          .technologies(technologies(root.path("technologies"), haystack))
          .analyzerVersion(JobRequirementsAnalyzer.VERSION)
          .build();
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not parse job analysis response", exception);
    }
  }

  private Requirement toRequirement(JsonNode item, String haystack) {
    String text = text(item, "text");
    String importance = enumValue(item.path("importance"), IMPORTANCE);
    String category = enumValue(item.path("category"), CATEGORY);
    String evidence = verbatim(haystack, text(item, "evidence"));
    if (text == null || importance == null || evidence == null)
      return null;

    Integer minYears = yearsOfExperience(item.path("minYears"), text + " " + evidence);
    return new Requirement(
        text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text,
        importance,
        category == null ? "other" : category,
        minYears,
        technologies(item.path("technologies"), haystack),
        evidence);
  }

  private static List<String> technologies(JsonNode node, String haystack) {
    if (!node.isArray())
      return List.of();
    String lowerSource = haystack.toLowerCase(Locale.ROOT);
    Set<String> kept = new LinkedHashSet<>();
    for (JsonNode technology : node) {
      String name = technology.isTextual() ? technology.asText().trim() : null;
      if (name != null && !name.isEmpty() && name.length() <= 80 && lowerSource.contains(name.toLowerCase(Locale.ROOT)))
        kept.add(name);
    }
    return List.copyOf(kept);
  }

  private static String enumValue(JsonNode node, Set<String> allowed) {
    if (!node.isTextual())
      return null;
    String value = node.asText().trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    if (allowed.contains(value))
      return value;
    // Modelos menores respondem no idioma da vaga ou com sinônimos.
    if (allowed.contains("required") && Set.of("obrigatorio", "obrigatório", "mandatory", "must", "must_have", "essential").contains(value))
      return "required";
    if (allowed.contains("preferred") && Set.of("desejavel", "desejável", "diferencial", "nice_to_have", "optional", "plus", "bonus").contains(value))
      return "preferred";
    return null;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
  }

  /** Devolve o trecho como aparece no texto da vaga (tolera diferenças de espaço) ou nulo. */
  static String verbatim(String source, String excerpt) {
    if (excerpt == null || excerpt.isBlank())
      return null;
    int exact = source.indexOf(excerpt);
    if (exact >= 0)
      return source.substring(exact, exact + excerpt.length());

    StringBuilder regex = new StringBuilder();
    for (String token : excerpt.trim().split("\\s+")) {
      if (!regex.isEmpty())
        regex.append("\\s+");
      regex.append(Pattern.quote(token));
    }
    Matcher matcher = Pattern.compile(regex.toString()).matcher(source);
    return matcher.find() ? source.substring(matcher.start(), matcher.end()) : null;
  }
}
