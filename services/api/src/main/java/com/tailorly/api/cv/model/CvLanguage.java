package com.tailorly.api.cv.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.tailorly.api.job.dto.JobPostingResponse;

/**
 * Idioma em que o currículo final é escrito. Só há dois: português do Brasil e inglês. A preferência da
 * pessoa ({@link #AUTO}, {@link #PT_BR_CODE} ou {@link #EN_CODE}) é resolvida para um deles em cada
 * geração; com {@code auto}, vale o idioma em que a vaga foi escrita. O idioma controla títulos de seção,
 * rótulos fixos e a língua pedida à IA na reescrita; fatos que a IA não reescreve ficam como aprovados.
 */
public enum CvLanguage {
  PT_BR("pt-BR", "Brazilian Portuguese"),
  EN("en", "English");

  public static final String AUTO = "auto";
  public static final String PT_BR_CODE = "pt-BR";
  public static final String EN_CODE = "en";
  /** Valores aceitos na preferência; usado nas validações de entrada. */
  public static final String PREFERENCE_PATTERN = "auto|pt-BR|en";

  private static final Pattern WORD = Pattern.compile("[a-z]+");
  private static final Set<String> PT_WORDS = Set.of("de", "da", "do", "das", "dos", "para", "com", "em", "uma",
      "um", "que", "por", "como", "nos", "nas", "ser", "sua", "seu", "ou", "experiencia", "conhecimento",
      "desenvolvimento", "vaga", "equipe", "empresa", "requisitos", "atividades", "voce", "sobre", "mais",
      "anos", "trabalho", "solucoes", "desejavel", "diferencial", "habilidades", "profissional", "area");
  private static final Set<String> EN_WORDS = Set.of("the", "and", "of", "to", "for", "with", "in", "you", "our",
      "we", "will", "are", "is", "or", "as", "on", "experience", "knowledge", "development", "team", "company",
      "requirements", "responsibilities", "skills", "years", "work", "solutions", "ability", "strong",
      "preferred", "about", "role", "from", "have", "be", "an", "that");

  private final String code;
  private final String promptName;

  CvLanguage(String code, String promptName) {
    this.code = code;
    this.promptName = promptName;
  }

  /** {@code pt-BR} ou {@code en}. */
  public String code() {
    return code;
  }

  /** Nome do idioma como entra nos prompts da IA. */
  public String promptName() {
    return promptName;
  }

  public boolean isEnglish() {
    return this == EN;
  }

  /** Preferência válida ({@code auto}, {@code pt-BR} ou {@code en}); qualquer outro valor, inclusive nulo, vira {@code auto}. */
  public static String normalizePreference(String preference) {
    if (PT_BR_CODE.equalsIgnoreCase(preference)) {
      return PT_BR_CODE;
    }
    if (EN_CODE.equalsIgnoreCase(preference)) {
      return EN_CODE;
    }
    return AUTO;
  }

  /** Resolve a preferência: idioma fixo vale sempre; {@code auto} detecta pelo texto da vaga. */
  public static CvLanguage resolve(String preference, String jobText) {
    return switch (normalizePreference(preference)) {
      case PT_BR_CODE -> PT_BR;
      case EN_CODE -> EN;
      default -> detect(jobText);
    };
  }

  /** O texto da vaga que serve à detecção: título, descrição, requisitos e responsabilidades. */
  public static String jobText(JobPostingResponse details) {
    if (details == null) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    append(text, details.title());
    append(text, details.descriptionText());
    for (var list : java.util.List.of(nonNull(details.responsibilities()), nonNull(details.requirements()),
        nonNull(details.niceToHave()))) {
      list.forEach(item -> append(text, item));
    }
    return text.toString();
  }

  /**
   * Detecta o idioma contando palavras funcionais de cada língua. Empate, texto vazio ou texto sem sinal
   * claro resultam em português do Brasil, o idioma padrão do produto.
   */
  public static CvLanguage detect(String text) {
    if (text == null || text.isBlank()) {
      return PT_BR;
    }
    String folded = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    int portuguese = 0;
    int english = 0;
    var matcher = WORD.matcher(folded);
    while (matcher.find()) {
      String word = matcher.group();
      if (PT_WORDS.contains(word)) {
        portuguese++;
      }
      if (EN_WORDS.contains(word)) {
        english++;
      }
    }
    return english > portuguese ? EN : PT_BR;
  }

  // ------------------------------------------------------------- rótulos

  public String heading(String sectionKey) {
    return switch (sectionKey) {
      case "technologies" -> isEnglish() ? "Technologies" : "Tecnologias";
      case "experience" -> isEnglish() ? "Professional Experience" : "Experiência profissional";
      case "projects" -> isEnglish() ? "Projects" : "Projetos";
      case "degrees" -> isEnglish() ? "Education" : "Formação acadêmica";
      case "certifications" -> isEnglish() ? "Certifications" : "Certificações";
      case "languages" -> isEnglish() ? "Languages" : "Idiomas";
      case "volunteering" -> isEnglish() ? "Volunteering" : "Voluntariado";
      default -> sectionKey;
    };
  }

  public String result() {
    return isEnglish() ? "Result: " : "Resultado: ";
  }

  public String current() {
    return isEnglish() ? "Present" : "Atual";
  }

  public String inProgress() {
    return isEnglish() ? "In progress" : "Em andamento";
  }

  public String credential() {
    return isEnglish() ? "Credential: " : "Credencial: ";
  }

  public String validUntil() {
    return isEnglish() ? "Valid until " : "Válida até ";
  }

  private static java.util.List<String> nonNull(java.util.List<String> list) {
    return list == null ? java.util.List.of() : list;
  }

  private static void append(StringBuilder text, String value) {
    if (value != null && !value.isBlank()) {
      text.append(value).append('\n');
    }
  }
}
