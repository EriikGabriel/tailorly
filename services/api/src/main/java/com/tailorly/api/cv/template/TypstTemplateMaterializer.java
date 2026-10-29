package com.tailorly.api.cv.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.model.CvDocument;

/**
 * Coloca uma cópia legível dos textos aprovados no código Typst. O template continua controlando
 * o layout, mas quem edita consegue encontrar e alterar os valores sem alternar para um JSON.
 */
public final class TypstTemplateMaterializer {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String DATA_BINDING = "#let cv = json(\"data.json\")";

  private TypstTemplateMaterializer() {
  }

  public static String materialize(String source, CvDocument document) {
    if (!source.contains(DATA_BINDING)) {
      return source;
    }
    try {
      JsonNode json = MAPPER.valueToTree(document);
      return source.replace(DATA_BINDING, "#let cv = " + typst(json));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Could not materialize the CV template", exception);
    }
  }

  private static String typst(JsonNode node) {
    if (node.isTextual()) {
      return quote(node.textValue());
    }
    if (node.isNull()) {
      return "none";
    }
    if (node.isBoolean() || node.isNumber()) {
      return node.toString();
    }
    if (node.isArray()) {
      StringBuilder result = new StringBuilder("(");
      for (int index = 0; index < node.size(); index++) {
        if (index > 0) {
          result.append(", ");
        }
        result.append(typst(node.get(index)));
      }
      if (node.size() == 1) {
        result.append(",");
      }
      return result.append(")").toString();
    }
    StringBuilder result = new StringBuilder("(");
    node.fields().forEachRemaining(field -> {
      if (result.length() > 1) {
        result.append(", ");
      }
      result.append(field.getKey()).append(": ").append(typst(field.getValue()));
    });
    return result.append(")").toString();
  }

  private static String quote(String value) {
    return '"' + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + '"';
  }
}
