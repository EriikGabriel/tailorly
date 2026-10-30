package com.tailorly.api.cv;

import java.text.Normalizer;
import java.util.Locale;

/** Nome do arquivo baixado: {@code curriculo-<empresa>.pdf}, sem acentos nem caracteres especiais. */
final class CvFilenames {
  private static final int MAX_SLUG_LENGTH = 40;

  private CvFilenames() {
  }

  static String of(CvGeneration generation, String extension) {
    var job = generation.getJobPostingVersion().getDetails();
    String label = job.company() != null && job.company().name() != null ? job.company().name() : job.title();
    String slug = label == null ? "" : Normalizer.normalize(label, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    if (slug.length() > MAX_SLUG_LENGTH) {
      slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
    }
    return "curriculo" + (slug.isEmpty() ? "" : "-" + slug) + "." + extension;
  }
}
