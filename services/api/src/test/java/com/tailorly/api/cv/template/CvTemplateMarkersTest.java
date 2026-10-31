package com.tailorly.api.cv.template;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Cada template embarcado precisa conter os marcadores que {@code TypstTemplateMaterializer}
 * substitui. Sem eles, a geração sai sem conteúdo ou sem o tamanho de fonte da densidade, sem erro.
 */
class CvTemplateMarkersTest {
  private static final List<String> MARKERS = List.of("// tailorly:compact", "// tailorly:title",
      "// tailorly:font-size", "// tailorly:content");

  @Test
  void everyBundledTemplateHasAllMaterializerMarkers() throws IOException {
    for (String name : CvTemplateCatalog.names()) {
      String source = read("cv-templates/" + name + "-v1.typ");
      for (String marker : MARKERS) {
        assertTrue(source.contains(marker), name + " is missing " + marker);
      }
    }
  }

  private static String read(String resource) throws IOException {
    try (InputStream stream = CvTemplateMarkersTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertTrue(stream != null, "missing resource " + resource);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
