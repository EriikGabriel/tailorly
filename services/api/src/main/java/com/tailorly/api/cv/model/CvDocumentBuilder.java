package com.tailorly.api.cv.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailorly.api.cv.model.CvDocument.Contact;
import com.tailorly.api.cv.model.CvDocument.Entry;
import com.tailorly.api.cv.model.CvDocument.Section;

/**
 * Monta o {@link CvDocument} só com valores aprovados. A vaga influencia apenas a ordem de
 * projetos e tecnologias que a pessoa já tem; nenhum item é criado, removido ou reescrito, e os
 * termos da vaga que a pessoa não tem aprovados não aparecem em lugar nenhum.
 */
public final class CvDocumentBuilder {
  private static final Pattern ITEM_KEY = Pattern.compile("^(\\d{1,3}):(.+)$");
  private static final Pattern YEAR_MONTH = Pattern.compile("^(\\d{4})-(\\d{2})$");
  private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
  private static final Pattern DOMAIN_LIKE = Pattern.compile("^[^\\s/@]+\\.[^\\s/@]{2,}(/\\S*)?$");

  private CvDocumentBuilder() {
  }

  /**
   * @param matchedTerms termos da vaga que aparecem nos fatos aprovados, para registrar o que a
   *                     vaga pediu e a pessoa de fato tem
   */
  public record Result(CvDocument document, List<String> matchedTerms) {
  }

  private record ItemKey(int recordOrder, int index) implements Comparable<ItemKey> {
    @Override
    public int compareTo(ItemKey other) {
      return Comparator.comparingInt(ItemKey::recordOrder).thenComparingInt(ItemKey::index).compare(this, other);
    }
  }

  public static Result build(List<FactValue> facts, JobTerms terms) {
    Map<String, String> identity = new LinkedHashMap<>();
    Map<String, TreeMap<ItemKey, Map<String, String>>> lists = new LinkedHashMap<>();
    StringBuilder allText = new StringBuilder();

    List<FactValue> ordered = facts.stream()
        .filter(fact -> fact.value() != null && !fact.value().isBlank())
        .sorted(Comparator.comparingInt(FactValue::recordOrder).thenComparing(FactValue::fieldKey))
        .toList();
    for (FactValue fact : ordered) {
      String value = fact.value().strip();
      allText.append(value).append('\n');
      if ("identity".equals(fact.category())) {
        identity.putIfAbsent(fact.fieldKey(), value);
        continue;
      }
      Matcher key = ITEM_KEY.matcher(fact.fieldKey());
      int index = key.matches() ? Integer.parseInt(key.group(1)) : 0;
      String field = key.matches() ? key.group(2).strip() : fact.fieldKey();
      lists.computeIfAbsent(fact.category(), category -> new TreeMap<>())
          .computeIfAbsent(new ItemKey(fact.recordOrder(), index), item -> new LinkedHashMap<>())
          .putIfAbsent(field, value);
    }

    List<Section> sections = new ArrayList<>();
    addSection(sections, "technologies", "Tecnologias", true,
        sortByRelevance(entries(lists, "technologies", CvDocumentBuilder::technology), terms));
    addSection(sections, "experience", "Experiência profissional", false,
        entries(lists, "experience", item -> role(item, "role", "company", "employment")));
    addSection(sections, "projects", "Projetos", false,
        sortByRelevance(entries(lists, "projects", CvDocumentBuilder::project), terms));
    addSection(sections, "degrees", "Formação acadêmica", false,
        entries(lists, "degrees", CvDocumentBuilder::degree));
    addSection(sections, "certifications", "Certificações", false,
        entries(lists, "certifications", CvDocumentBuilder::certification));
    addSection(sections, "languages", "Idiomas", true,
        entries(lists, "languages", item -> new Entry(item.get("language"), item.get("proficiency"),
            null, null, List.of(), null)));
    addSection(sections, "volunteering", "Voluntariado", false,
        entries(lists, "volunteering", item -> role(item, "role", "organization", null)));

    CvDocument document = new CvDocument(
        identity.get("name"), identity.get("headline"), identity.get("summary"),
        contacts(identity), List.copyOf(sections));
    return new Result(document, terms.matchedIn(allText.toString()));
  }

  private static void addSection(List<Section> sections, String key, String heading, boolean compact,
      List<Entry> entries) {
    if (!entries.isEmpty()) {
      sections.add(new Section(key, heading, compact, List.copyOf(entries)));
    }
  }

  private static List<Entry> entries(Map<String, TreeMap<ItemKey, Map<String, String>>> lists, String category,
      java.util.function.Function<Map<String, String>, Entry> mapper) {
    TreeMap<ItemKey, Map<String, String>> items = lists.get(category);
    if (items == null) {
      return List.of();
    }
    List<Entry> entries = new ArrayList<>();
    int ordinal = 0;
    for (Map<String, String> item : items.values()) {
      Entry entry = mapper.apply(item);
      // O identificador segue a ordem original do item, antes de qualquer reordenação por relevância.
      if (entry.title() != null) {
        entries.add(entry.withId(category + ":" + ordinal));
      }
      ordinal++;
    }
    return entries;
  }

  // ------------------------------------------------------------------ itens

  private static Entry role(Map<String, String> item, String roleKey, String organizationKey, String metaKey) {
    String role = item.get(roleKey);
    String organization = item.get(organizationKey);
    return new Entry(
        role != null ? role : organization,
        role != null ? organization : null,
        metaKey == null ? null : item.get(metaKey),
        period(item.get("start"), item.get("end"), item.get("status"), "Atual"),
        details(item.get("description")),
        null);
  }

  private static Entry technology(Map<String, String> item) {
    return new Entry(item.get("name"), item.get("context"), null, null, List.of(), null);
  }

  private static Entry project(Map<String, String> item) {
    List<String> details = new ArrayList<>(details(item.get("statement")));
    if (item.get("metric") != null) {
      details.add("Resultado: " + item.get("metric"));
    }
    String link = item.get("link");
    String url = url(link);
    if (link != null && url == null) {
      details.add(link);
    }
    return new Entry(item.get("title"), item.get("context"), null, null, List.copyOf(details), url);
  }

  private static Entry degree(Map<String, String> item) {
    return new Entry(item.get("degree"), item.get("institution"), null,
        period(item.get("start"), item.get("end"), item.get("status"), "Em andamento"), List.of(), null);
  }

  private static Entry certification(Map<String, String> item) {
    String expires = date(item.get("expires"));
    List<String> details = item.get("credential") == null
        ? List.of() : List.of("Credencial: " + item.get("credential"));
    return new Entry(item.get("name"), item.get("issuer"), expires == null ? null : "Válida até " + expires,
        date(item.get("issued")), details, null);
  }

  private static List<String> details(String text) {
    return text == null ? List.of() : List.of(text);
  }

  // ------------------------------------------------------------------ ordem

  private static List<Entry> sortByRelevance(List<Entry> entries, JobTerms terms) {
    if (terms.terms().isEmpty()) {
      return entries;
    }
    // Ordenação estável: itens igualmente relevantes mantêm a ordem do currículo.
    return entries.stream()
        .sorted(Comparator.comparingInt((Entry entry) -> -terms.matchedIn(searchable(entry)).size()))
        .toList();
  }

  private static String searchable(Entry entry) {
    List<String> parts = new ArrayList<>();
    parts.add(entry.title());
    parts.add(entry.subtitle());
    parts.addAll(entry.details());
    return String.join("\n", parts.stream().filter(java.util.Objects::nonNull).toList());
  }

  // ------------------------------------------------------- contato e datas

  private static List<Contact> contacts(Map<String, String> identity) {
    List<Contact> contacts = new ArrayList<>();
    if (identity.get("location") != null) {
      contacts.add(new Contact(identity.get("location"), null));
    }
    String email = identity.get("email");
    if (email != null) {
      contacts.add(new Contact(email, email.contains("@") && !email.contains(" ") ? "mailto:" + email : null));
    }
    if (identity.get("phone") != null) {
      contacts.add(new Contact(identity.get("phone"), null));
    }
    for (String key : List.of("linkedin", "github", "website")) {
      String value = identity.get(key);
      if (value != null) {
        contacts.add(new Contact(value, url(value)));
      }
    }
    return List.copyOf(contacts);
  }

  /** Link clicável quando o valor parece um endereço web; caso contrário, {@code null}. */
  static String url(String value) {
    if (value == null) {
      return null;
    }
    String text = value.strip();
    if (SCHEME.matcher(text).matches()) {
      return text.regionMatches(true, 0, "http://", 0, 7) || text.regionMatches(true, 0, "https://", 0, 8)
          ? text : null;
    }
    return DOMAIN_LIKE.matcher(text).matches() ? "https://" + text : null;
  }

  private static String period(String start, String end, String status, String currentLabel) {
    String from = date(start);
    String to = date(end);
    if (to == null && currentLabel.equals(status)) {
      to = currentLabel;
    }
    if (from == null) {
      return to;
    }
    return to == null ? from : from + " – " + to;
  }

  /** {@code 2021-05} vira {@code 05/2021}; qualquer outro formato é mantido como foi aprovado. */
  static String date(String value) {
    if (value == null) {
      return null;
    }
    Matcher match = YEAR_MONTH.matcher(value.strip());
    return match.matches() ? match.group(2) + "/" + match.group(1) : value.strip();
  }
}
