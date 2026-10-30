package com.tailorly.api.job.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class HtmlTextTest {
  private static List<String> texts(String html) {
    return HtmlText.fromMarkupOrText(html).stream().map(HtmlText.Line::text).toList();
  }

  @Test
  void keepsTechnologyNamesWithDotsIntact() {
    assertEquals(List.of("Experiência com Node.js, Vue.Js, ASP.NET, ASP.Net e .NET Core."),
        texts("Experiência com Node.js, Vue.Js, ASP.NET, ASP.Net e .NET Core."));
  }

  @Test
  void splitsSentencesGluedAfterWordsAcronymsAndParentheses() {
    assertEquals(List.of("Conhecer Java.", "Dominar CI/CD.", "Usar Kafka (ou RabbitMQ).", "Falar inglês."),
        texts("Conhecer Java.Dominar CI/CD.Usar Kafka (ou RabbitMQ).Falar inglês."));
  }

  @Test
  void listItemsAndParagraphsBecomeSeparateLines() {
    assertEquals(List.of("Intro", "Item um", "Item dois"),
        texts("<p>Intro</p><ul><li>Item um</li><li>Item <b>dois</b></li></ul>"));
  }

  @Test
  void marksHeadingsAndFullyBoldLines() {
    List<HtmlText.Line> lines = HtmlText.fromMarkupOrText(
        "<h2>Requisitos</h2><p><strong>Negrito</strong></p><p>Texto <strong>parcial</strong></p>");

    assertTrue(lines.get(0).heading() && lines.get(0).emphasized());
    assertTrue(lines.get(1).emphasized());
    assertFalse(lines.get(1).heading());
    assertFalse(lines.get(2).emphasized());
  }

  @Test
  void decodesEscapedMarkupAndHandlesPlainText() {
    assertEquals(List.of("Olá", "Mundo"), texts("&lt;p&gt;Olá&lt;/p&gt;&lt;p&gt;Mundo&lt;/p&gt;"));
    assertEquals(List.of("Linha 1", "Linha 2"), texts("Linha 1\n\n  Linha 2  \n"));
    assertEquals(List.of(), texts("   "));
    assertEquals(List.of(), texts(null));
  }

  @Test
  void stripsBulletMarkers() {
    assertEquals("Item", HtmlText.stripBullet("•  Item"));
    assertEquals("Item", HtmlText.stripBullet("- Item"));
    assertEquals("5 anos", HtmlText.stripBullet("5 anos"));
  }
}
