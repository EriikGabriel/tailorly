package com.tailorly.api.cv.model;

/**
 * Um fato aprovado, no formato mínimo que a montagem do currículo precisa. {@code recordOrder} é a
 * ordem do agrupador e {@code fieldKey} traz o índice do item nas listas ({@code "0:company"}).
 */
public record FactValue(String category, int recordOrder, String fieldKey, String value) {
}
