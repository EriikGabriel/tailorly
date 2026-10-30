package com.tailorly.api.cv.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tailorly.api.shared.util.Sha256;

/**
 * Templates Typst embarcados na aplicação e seu registro versionado no banco.
 *
 * <p>Cada arquivo em {@code cv-templates/} é uma versão fechada: ao gerar, a versão mais recente
 * do nome pedido é registrada em {@code cv_templates} se ainda não existir. Se a versão já existe
 * com outro conteúdo, o arquivo foi editado depois de usado e a geração falha, pois as gerações
 * antigas deixariam de corresponder ao template que lhes é atribuído. Mude o layout criando o
 * arquivo da versão seguinte e acrescentando-o a {@link #BUNDLED}.</p>
 */
@Service
@RequiredArgsConstructor
public class CvTemplateCatalog {
  public static final String DEFAULT_NAME = "classic";

  private record Bundled(String name, int version, String resource) {
  }

  private static final List<Bundled> BUNDLED = List.of(
      new Bundled("classic", 1, "cv-templates/classic-v1.typ"));

  private final CvTemplateRepository templates;

  public static boolean exists(String name) {
    return BUNDLED.stream().anyMatch(template -> template.name().equals(name));
  }

  /** Devolve a versão mais recente do template, registrando-a no banco na primeira utilização. */
  @Transactional
  public CvTemplate latest(String name) {
    Bundled bundled = BUNDLED.stream()
        .filter(template -> template.name().equals(name))
        .max(Comparator.comparingInt(Bundled::version))
        .orElseThrow(() -> new IllegalArgumentException("Unknown CV template: " + name));
    String content = read(bundled.resource());
    String hash = Sha256.hex(content);

    return templates.findByNameAndVersionAndOwnerIsNull(bundled.name(), bundled.version())
        .map(existing -> {
          if (!existing.getContentHash().equals(hash)) {
            throw new IllegalStateException("CV template %s v%d was edited after being registered; publish a new version"
                .formatted(bundled.name(), bundled.version()));
          }
          return existing;
        })
        .orElseGet(() -> templates.save(CvTemplate.builder()
            .name(bundled.name())
            .version(bundled.version())
            .content(content)
            .contentHash(hash)
            .createdAt(Instant.now())
            .build()));
  }

  private static String read(String resource) {
    try (InputStream stream = CvTemplateCatalog.class.getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IllegalStateException("Missing CV template resource " + resource);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read CV template resource " + resource, exception);
    }
  }
}
