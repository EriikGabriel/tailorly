package com.tailorly.api.cv;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Nome do arquivo baixado: {@code <primeiro-nome>_<cargo>_<empresa>.<ext>} (por exemplo
 * {@code erik_desenvolvedor-backend_acme.pdf}), sem acentos nem caracteres especiais. As partes que
 * faltam são omitidas; sem nenhuma delas o nome cai em {@code curriculo.<ext>}.
 */
final class CvFilenames {
  private static final int MAX_PART_LENGTH = 30;
  private static final String FALLBACK = "curriculo";

  private CvFilenames() {
  }

  static String of(CvGeneration generation, String extension) {
    var job = generation.getJobPostingVersion().getDetails();
    String company = job.company() != null ? job.company().name() : null;
    String name = generation.getDocument() != null ? generation.getDocument().name() : null;
    return build(name, job.title(), company, extension);
  }

  static String build(String fullName, String jobTitle, String company, String extension) {
    List<String> parts = Stream.of(slug(firstName(fullName)), slug(cleanTitle(jobTitle)), slug(company))
        .filter(Objects::nonNull)
        .toList();
    return (parts.isEmpty() ? FALLBACK : String.join("_", parts)) + "." + extension;
  }

  private static String firstName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return null;
    }
    return fullName.strip().split("\\s+")[0];
  }

  /** Tira o que costuma vir depois do cargo no anúncio: "(Remoto)", "- São Paulo", "| Pleno". */
  private static String cleanTitle(String title) {
    if (title == null) {
      return null;
    }
    return title.replaceAll("\\([^)]*\\)|\\[[^]]*]", " ").split("\\s[-–—|/]\\s", 2)[0];
  }

  private static String slug(String label) {
    if (label == null) {
      return null;
    }
    String slug = Normalizer.normalize(label, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    if (slug.length() > MAX_PART_LENGTH) {
      slug = slug.substring(0, MAX_PART_LENGTH).replaceAll("-+$", "");
    }
    return slug.isEmpty() ? null : slug;
  }
}
