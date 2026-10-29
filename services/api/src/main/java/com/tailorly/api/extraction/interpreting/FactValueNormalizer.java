package com.tailorly.api.extraction.interpreting;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Padroniza o valor extraído de cada fato para os formatos que a revisão no frontend reconhece
 * e edita com controles próprios (calendário, lista de vínculos, nível de proficiência etc.).
 *
 * <p>Só o {@code extractedValue} muda; o trecho de evidência continua sendo o texto original do
 * documento. Valores que não têm um equivalente conhecido (ex.: um nível "B2" ou um vínculo
 * incomum) são preservados como foram escritos, nunca descartados nem adivinhados.</p>
 *
 * <p>Formatos canônicos: datas {@code YYYY-MM} (ou {@code YYYY} quando só o ano consta);
 * situação do vínculo {@code Atual}/{@code Encerrado}; situação do curso
 * {@code Em andamento}/{@code Concluído}; proficiência {@code Básico}, {@code Intermediário},
 * {@code Avançado}, {@code Fluente} ou {@code Nativo}; telefone brasileiro
 * {@code (11) 98765-4321}; links sem pontuação ou barra finais.</p>
 */
public final class FactValueNormalizer {
  private static final Pattern ITEM_PREFIX = Pattern.compile("^(\\d{1,3}):(.+)$");
  private static final Set<String> DATE_KEYS = Set.of("start", "end", "issued", "expires");
  private static final Set<String> LINK_KEYS = Set.of("linkedin", "github", "website", "link");
  private static final Set<String> MULTILINE_KEYS = Set.of("description", "statement", "summary");

  private static final Pattern CURRENT = Pattern.compile(
      "^(atual|atualmente|presente|current|currently|present|now|hoje|ate hoje|ate o momento|em andamento|em curso|cursando|ongoing)\\b.*");

  private static final Pattern ISO_DATE = Pattern.compile("^(\\d{4})-(\\d{1,2})(?:-\\d{1,2})?(?:[t ].*)?$");
  private static final Pattern YEAR = Pattern.compile("^(\\d{4})$");
  private static final Pattern MONTH_YEAR = Pattern.compile("^(\\d{1,2})\\s*[/.-]\\s*(\\d{4})$");
  private static final Pattern YEAR_MONTH = Pattern.compile("^(\\d{4})\\s*[/.]\\s*(\\d{1,2})$");
  private static final Pattern DAY_MONTH_YEAR = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$");
  private static final Pattern NAME_YEAR = Pattern.compile("^([a-z]{3,9})\\.?(?:\\s+de\\s+|\\s*[/-]\\s*|\\s+)(\\d{4})$");
  private static final Pattern DAY_NAME_YEAR = Pattern.compile("^(\\d{1,2})\\s+(?:de\\s+)?([a-z]{3,9})\\.?\\s+(?:de\\s+)?(\\d{4})$");

  private static final Map<String, Integer> MONTHS = monthNames();

  private FactValueNormalizer() {
  }

  /**
   * Normaliza a lista inteira: valores, período (término "atual" vira situação) e duplicatas.
   * Propostas cujo valor não é um texto utilizável são descartadas.
   */
  public static List<FactProposal> normalize(List<FactProposal> proposals) {
    List<FactProposal> normalized = new ArrayList<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      String value = normalizeValue(proposal.category(), key.base(), proposal.extractedValue());
      if (value != null)
        normalized.add(withValue(proposal, proposal.fieldKey(), value));
    }

    return dedupe(currentEndToStatus(relocateVolunteering(normalized)));
  }

  /** Normaliza um único valor; {@code null} quando não há um texto utilizável. */
  public static String normalizeValue(String category, String baseKey, Object raw) {
    String text = asText(raw);
    if (text == null)
      return null;

    String value = MULTILINE_KEYS.contains(baseKey) ? tidyMultiline(text) : tidy(text);
    if (value.isEmpty())
      return null;

    if (DATE_KEYS.contains(baseKey))
      return normalizeDate(value);

    return switch (baseKey) {
      case "status" -> normalizeStatus(category, value);
      case "employment" -> normalizeEmployment(value);
      case "proficiency" -> normalizeProficiency(value);
      case "phone" -> normalizePhone(value);
      case "email" -> stripEmail(value);
      default -> LINK_KEYS.contains(baseKey) ? normalizeLink(value) : value;
    };
  }

  // ------------------------------------------------------------------ datas

  static String normalizeDate(String value) {
    String text = fold(value).replaceAll("\\.+$", "");

    Matcher match = ISO_DATE.matcher(text);
    if (match.matches())
      return yearMonth(match.group(1), match.group(2), value);

    if (YEAR.matcher(text).matches())
      return text;

    match = MONTH_YEAR.matcher(text);
    if (match.matches())
      return yearMonth(match.group(2), match.group(1), value);

    match = YEAR_MONTH.matcher(text);
    if (match.matches())
      return yearMonth(match.group(1), match.group(2), value);

    match = DAY_MONTH_YEAR.matcher(text);
    if (match.matches())
      return yearMonth(match.group(3), match.group(2), value);

    match = NAME_YEAR.matcher(text);
    if (match.matches() && MONTHS.containsKey(match.group(1)))
      return yearMonth(match.group(2), String.valueOf(MONTHS.get(match.group(1))), value);

    match = DAY_NAME_YEAR.matcher(text);
    if (match.matches() && MONTHS.containsKey(match.group(2)))
      return yearMonth(match.group(3), String.valueOf(MONTHS.get(match.group(2))), value);

    return value;
  }

  private static String yearMonth(String year, String month, String original) {
    int m = Integer.parseInt(month);
    int y = Integer.parseInt(year);
    if (m < 1 || m > 12 || y < 1900 || y > 2200)
      return original;
    return "%04d-%02d".formatted(y, m);
  }

  static boolean isCurrent(String value) {
    return CURRENT.matcher(fold(value)).matches();
  }

  // ------------------------------------------------------- campos enumerados

  private static String normalizeStatus(String category, String value) {
    String text = fold(value);
    boolean current = isCurrent(value);

    if ("degrees".equals(category)) {
      if (current)
        return "Em andamento";
      if (containsAny(text, "conclu", "complet", "formad", "gradua", "finaliz", "finish"))
        return "Concluído";
      return value;
    }

    if (current)
      return "Atual";
    if (containsAny(text, "encerr", "finaliz", "ended", "former", "past", "anterior", "desligad", "left"))
      return "Encerrado";
    return value;
  }

  private static String normalizeEmployment(String value) {
    String text = fold(value);
    if (containsAny(text, "estag") || hasWord(text, "intern", "internship"))
      return "Estágio";
    if (hasWord(text, "clt") || containsAny(text, "carteira assinada"))
      return "CLT";
    if (hasWord(text, "pj", "b2b", "contractor") || containsAny(text, "pessoa juridica"))
      return "PJ";
    if (containsAny(text, "trainee"))
      return "Trainee";
    if (containsAny(text, "aprendiz", "apprentice"))
      return "Aprendiz";
    if (containsAny(text, "temporar", "fixed-term", "fixed term"))
      return "Temporário";
    if (containsAny(text, "freelanc", "autonom", "self-employed", "self employed"))
      return "Autônomo / Freelancer";
    return value;
  }

  private static String normalizeProficiency(String value) {
    String text = fold(value);
    if (containsAny(text, "nativ", "bilingu", "mother tongue"))
      return "Nativo";
    if (containsAny(text, "fluen", "fluid"))
      return "Fluente";
    if (containsAny(text, "avanc", "advanc"))
      return "Avançado";
    if (containsAny(text, "intermed"))
      return "Intermediário";
    if (containsAny(text, "basic", "inicia", "element", "beginner"))
      return "Básico";
    return value;
  }

  // ---------------------------------------------------- contato e links

  /** Telefone brasileiro como no frontend; números internacionais ou atípicos ficam como escritos. */
  static String normalizePhone(String value) {
    String text = value.trim();
    if (text.startsWith("+") && !text.startsWith("+55"))
      return text;

    String digits = text.replaceAll("\\D", "");
    boolean countryCode = false;
    if (text.startsWith("+55") || (digits.startsWith("55") && digits.length() >= 12)) {
      digits = digits.substring(2);
      countryCode = true;
    }

    String masked;
    if (digits.length() == 11)
      masked = "(%s) %s-%s".formatted(digits.substring(0, 2), digits.substring(2, 7), digits.substring(7));
    else if (digits.length() == 10)
      masked = "(%s) %s-%s".formatted(digits.substring(0, 2), digits.substring(2, 6), digits.substring(6));
    else
      return text;
    return countryCode ? "+55 " + masked : masked;
  }

  private static String stripEmail(String value) {
    return value.replaceFirst("(?i)^mailto:", "").replaceAll("^[<(\\s]+|[>)\\s.,;]+$", "");
  }

  private static String normalizeLink(String value) {
    String link = value.replaceFirst("(?i)^mailto:", "").replaceAll("^[<(\\s]+|[>)\\s.,;]+$", "");
    while (link.endsWith("/"))
      link = link.substring(0, link.length() - 1);
    return link;
  }

  // ------------------------------------------------- consistência da lista

  /**
   * Rede de segurança: um item de experiência cujo vínculo o modelo marcou como voluntário não é
   * emprego. O item inteiro (todos os fatos do mesmo índice) vira {@code volunteering}, com
   * {@code company} como {@code organization}, em índices novos que não colidem com os existentes.
   */
  private static List<FactProposal> relocateVolunteering(List<FactProposal> proposals) {
    Set<String> volunteerItems = new LinkedHashSet<>();
    int highestVolunteerIndex = -1;
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      if (proposal.category().equals("volunteering") && key.index() != null)
        highestVolunteerIndex = Math.max(highestVolunteerIndex, Integer.parseInt(key.index()));
      if (proposal.category().equals("experience") && key.index() != null && key.base().equals("employment")
          && isVolunteerWork(String.valueOf(proposal.extractedValue())))
        volunteerItems.add(key.index());
    }
    if (volunteerItems.isEmpty())
      return proposals;

    Map<String, Integer> newIndexes = new HashMap<>();
    int next = highestVolunteerIndex + 1;
    for (String index : volunteerItems)
      newIndexes.put(index, next++);

    List<FactProposal> result = new ArrayList<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      if (!proposal.category().equals("experience") || key.index() == null || !newIndexes.containsKey(key.index())) {
        result.add(proposal);
        continue;
      }
      if (key.base().equals("employment"))
        continue;
      String base = key.base().equals("company") ? "organization" : key.base();
      result.add(new FactProposal("volunteering", newIndexes.get(key.index()) + ":" + base,
          proposal.extractedValue(), proposal.extractedPageId(), proposal.sourceExcerpt(),
          proposal.excerptStart(), proposal.excerptEnd(), proposal.needsReview(), proposal.reviewReason()));
    }
    return result;
  }

  private static boolean isVolunteerWork(String employment) {
    return containsAny(fold(employment), "voluntar", "volunteer");
  }

  /** Um término como "Atual" não é uma data: vira a situação do item (se ela ainda não existir). */
  private static List<FactProposal> currentEndToStatus(List<FactProposal> proposals) {
    Set<String> statusKeys = new HashSet<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      if (key.base().equals("status"))
        statusKeys.add(proposal.category() + "|" + proposal.fieldKey());
    }

    List<FactProposal> result = new ArrayList<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      boolean periodItem = proposal.category().equals("experience") || proposal.category().equals("volunteering")
          || proposal.category().equals("degrees");
      if (periodItem && key.base().equals("end") && isCurrent(String.valueOf(proposal.extractedValue()))) {
        String statusKey = key.withBase("status");
        if (statusKeys.add(proposal.category() + "|" + statusKey)) {
          String status = proposal.category().equals("degrees") ? "Em andamento" : "Atual";
          result.add(withValue(proposal, statusKey, status));
        }
        continue;
      }
      result.add(proposal);
    }
    return result;
  }

  /**
   * Remove fatos idênticos e, em tecnologias e idiomas, itens repetidos (mesmo nome em índices
   * diferentes) junto com os demais fatos do item repetido.
   */
  private static List<FactProposal> dedupe(List<FactProposal> proposals) {
    Map<String, String> firstItemByName = new LinkedHashMap<>();
    Set<String> droppedItems = new HashSet<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      boolean identifying = (proposal.category().equals("technologies") && key.base().equals("name"))
          || (proposal.category().equals("languages") && key.base().equals("language"));
      if (!identifying || key.index() == null)
        continue;
      String name = proposal.category() + "|" + fold(String.valueOf(proposal.extractedValue()));
      String owner = firstItemByName.putIfAbsent(name, key.index());
      if (owner != null && !owner.equals(key.index()))
        droppedItems.add(proposal.category() + "|" + key.index());
    }

    Set<String> seen = new HashSet<>();
    List<FactProposal> result = new ArrayList<>();
    for (FactProposal proposal : proposals) {
      Key key = Key.of(proposal.fieldKey());
      if (key.index() != null && droppedItems.contains(proposal.category() + "|" + key.index()))
        continue;
      String identity = proposal.category() + "|" + proposal.fieldKey() + "|" + proposal.extractedValue();
      if (seen.add(identity))
        result.add(proposal);
    }
    return result;
  }

  // ---------------------------------------------------------------- utilitários

  private static FactProposal withValue(FactProposal proposal, String fieldKey, String value) {
    return new FactProposal(proposal.category(), fieldKey, value, proposal.extractedPageId(),
        proposal.sourceExcerpt(), proposal.excerptStart(), proposal.excerptEnd(),
        proposal.needsReview(), proposal.reviewReason());
  }

  private static String asText(Object raw) {
    if (raw == null)
      return null;
    if (raw instanceof String text)
      return text;
    if (raw instanceof Boolean)
      return null;
    if (raw instanceof Number number) {
      double asDouble = number.doubleValue();
      return asDouble == Math.rint(asDouble) ? String.valueOf((long) asDouble) : number.toString();
    }
    if (raw instanceof Iterable<?> items) {
      List<String> parts = new ArrayList<>();
      for (Object item : items) {
        String part = asText(item);
        if (part == null)
          return null;
        parts.add(part.strip());
      }
      return String.join(", ", parts);
    }
    return null;
  }

  private static String tidy(String text) {
    return text.replace(' ', ' ').replaceAll("\\s+", " ").strip();
  }

  private static String tidyMultiline(String text) {
    return text.replace(' ', ' ').replaceAll("[ \\t]+", " ").replaceAll(" ?\\R ?", "\n").strip();
  }

  static String fold(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .strip()
        .toLowerCase(Locale.ROOT);
  }

  private static boolean hasWord(String text, String... words) {
    for (String word : words) {
      if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find())
        return true;
    }
    return false;
  }

  private static boolean containsAny(String text, String... parts) {
    for (String part : parts) {
      if (text.contains(part))
        return true;
    }
    return false;
  }

  private static Map<String, Integer> monthNames() {
    String[][] names = {
        {"jan", "janeiro", "january"}, {"fev", "fevereiro", "feb", "february"},
        {"mar", "marco", "march"}, {"abr", "abril", "apr", "april"},
        {"mai", "maio", "may"}, {"jun", "junho", "june"},
        {"jul", "julho", "july"}, {"ago", "agosto", "aug", "august"},
        {"set", "setembro", "sep", "sept", "september"}, {"out", "outubro", "oct", "october"},
        {"nov", "novembro", "november"}, {"dez", "dezembro", "dec", "december"}};
    Map<String, Integer> months = new LinkedHashMap<>();
    for (int i = 0; i < names.length; i++) {
      for (String name : names[i])
        months.put(name, i + 1);
    }
    return Map.copyOf(months);
  }

  /** Chave do fato, separando o índice do item ("1:company" -> índice 1, base company). */
  private record Key(String index, String base) {
    static Key of(String fieldKey) {
      Matcher match = ITEM_PREFIX.matcher(fieldKey);
      return match.matches() ? new Key(match.group(1), match.group(2)) : new Key(null, fieldKey);
    }

    String withBase(String newBase) {
      return index == null ? newBase : index + ":" + newBase;
    }
  }
}
