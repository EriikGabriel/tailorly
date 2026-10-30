package com.tailorly.api.extraction.interpreting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.ai.AiChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converte a resposta JSON do modelo em {@link FactProposal}. Propostas sem evidência verificável
 * na página citada são descartadas individualmente; se nenhuma sobreviver, a resposta inteira é
 * tratada como falha.
 */
final class InterpretationResponseParser {
  private static final Logger log = LoggerFactory.getLogger(InterpretationResponseParser.class);

  private final ObjectMapper objectMapper;

  InterpretationResponseParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  List<FactProposal> parseAndValidate(String response, List<ExtractedTextPage> pages, String provider) {
    try {
      JsonNode facts = objectMapper.readTree(AiChatClient.stripCodeFence(response)).path("facts");
      if (!facts.isArray())
        throw new IllegalStateException("AI response must contain a facts array");

      Map<Integer, ExtractedTextPage> pagesByNumber = new HashMap<>();

      pages.forEach(page -> pagesByNumber.put(page.pageNumber(), page));
      List<FactProposal> proposals = new ArrayList<>();

      for (JsonNode fact : facts) {
        FactProposal proposal = toProposal(fact, pagesByNumber);
        if (proposal != null)
          proposals.add(proposal);
      }

      // Fatos sem evidência verificável nunca são persistidos; descartá-los individualmente evita perder
      // o restante da resposta, mas se nenhum sobreviver a resposta inteira é tratada como falha.
      int discarded = facts.size() - proposals.size();
      if (discarded > 0) {
        log.warn("{} discarded {} of {} facts without verifiable source evidence", provider, discarded,
            facts.size());
      }
      if (!facts.isEmpty() && proposals.isEmpty())
        throw new IllegalStateException("AI response contained no fact with verifiable source evidence");

      return List.copyOf(proposals);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not parse " + provider + " curriculum interpretation response",
          exception);
    }
  }

  private static final int MAX_REVIEW_REASON_LENGTH = 280;

  private FactProposal toProposal(JsonNode fact, Map<Integer, ExtractedTextPage> pagesByNumber) {
    // Ignora propostas incompletas ou que apontem para uma página fora desta extração.
    Integer pageNumber = pageNumber(fact.path("pageNumber"));
    if (pageNumber == null)
      return null;

    ExtractedTextPage page = pagesByNumber.get(pageNumber);
    String category = CurriculumFactSchema.normalizeCategory(text(fact, "category"));
    String fieldKey = CurriculumFactSchema.normalizeFieldKey(category, text(fact, "fieldKey"));
    String excerpt = text(fact, "sourceExcerpt");
    JsonNode value = fact.get("extractedValue");

    if (page == null || page.text() == null || category == null || fieldKey == null
        || excerpt == null || excerpt.isBlank() || value == null || value.isNull())
      return null;

    // O modelo não fornece offsets confiáveis; só aceitamos trechos presentes na página e calculamos índices aqui.
    int[] range = ExcerptLocator.locate(page.text(), excerpt);
    if (range == null)
      return null;

    // O trecho persistido é sempre o texto original da página, nunca a versão reescrita pelo modelo.
    String verbatimExcerpt = page.text().substring(range[0], range[1]);
    Object extractedValue = objectMapper.convertValue(value, Object.class);
    // Campo ausente ou de tipo inesperado é tratado como pendência: dado não confiável nunca libera
    // um fato sem revisão por omissão.
    JsonNode needsReviewNode = fact.path("needsReview");
    boolean needsReview = !needsReviewNode.isBoolean() || needsReviewNode.asBoolean();
    String reason = needsReview ? text(fact, "reviewReason") : null;
    if (reason != null && reason.length() > MAX_REVIEW_REASON_LENGTH)
      reason = reason.substring(0, MAX_REVIEW_REASON_LENGTH);
    return new FactProposal(category, fieldKey, extractedValue,
        page.extractedPageId(), verbatimExcerpt, range[0], range[1], needsReview, reason);
  }

  private static Integer pageNumber(JsonNode node) {
    if (node.canConvertToInt())
      return node.asInt();
    if (node.isTextual() && node.asText().trim().matches("\\d{1,6}"))
      return Integer.valueOf(node.asText().trim());
    return null;
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
  }

}
