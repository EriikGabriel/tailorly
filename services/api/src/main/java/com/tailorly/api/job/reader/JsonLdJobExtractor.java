package com.tailorly.api.job.reader;

import static com.tailorly.api.job.reader.JobValues.blankToNull;
import static com.tailorly.api.job.reader.JobValues.httpUrl;
import static com.tailorly.api.job.reader.JobValues.text;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingResponse.Company;
import com.tailorly.api.job.dto.JobPostingResponse.Location;
import com.tailorly.api.job.dto.JobPostingResponse.Salary;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Lê o marcador schema.org {@code JobPosting} em JSON-LD, que muitos recrutadores publicam por causa
 * do Google Jobs (por exemplo Greenhouse e Lever).
 */
@Component
@RequiredArgsConstructor
public class JsonLdJobExtractor implements JobPageExtractor {
  private final ObjectMapper objectMapper;

  @Override
  public Optional<JobPostingResponse> extract(Document document, String finalUrl) {
    for (Element script : document.select("script[type=application/ld+json]")) {
      JsonNode root;
      try {
        root = objectMapper.readTree(script.data());
      } catch (JsonProcessingException exception) {
        continue;
      }
      JsonNode posting = findJobPosting(root);
      if (posting != null)
        return Optional.of(toResponse(posting));
    }
    return Optional.empty();
  }

  private static JsonNode findJobPosting(JsonNode node) {
    if (node == null)
      return null;
    if (node.isArray()) {
      for (JsonNode child : node) {
        JsonNode found = findJobPosting(child);
        if (found != null)
          return found;
      }
      return null;
    }
    if (!node.isObject())
      return null;
    if (hasType(node, "JobPosting"))
      return node;
    return findJobPosting(node.path("@graph"));
  }

  private static boolean hasType(JsonNode node, String type) {
    JsonNode declared = node.path("@type");
    if (declared.isArray()) {
      for (JsonNode candidate : declared) {
        if (type.equalsIgnoreCase(candidate.asText()))
          return true;
      }
      return false;
    }
    return type.equalsIgnoreCase(declared.asText());
  }

  private JobPostingResponse toResponse(JsonNode posting) {
    JobContentBuilder content = new JobContentBuilder();
    content.addPart(null, text(posting, "description"), JobSection.OTHER);
    content.addPart("Responsabilidades", text(posting, "responsibilities"), JobSection.RESPONSIBILITIES);
    content.addPart("Qualificações", text(posting, "qualifications"), JobSection.REQUIREMENTS);
    content.addPart("Benefícios", text(posting, "jobBenefits"), JobSection.BENEFITS);
    requirementItems(posting.path("experienceRequirements")).forEach(item -> content.addItem(JobSection.REQUIREMENTS, item));
    requirementItems(posting.path("educationRequirements")).forEach(item -> content.addItem(JobSection.REQUIREMENTS, item));

    List<String> warnings = new ArrayList<>();
    JobPostingResponse.JobPostingResponseBuilder builder = content.applyTo(JobPostingResponse.builder())
        .platform("json-ld")
        .externalId(identifier(posting.path("identifier")))
        .title(first(text(posting, "title"), text(posting, "name")))
        .company(company(posting.path("hiringOrganization")))
        .location(location(posting.path("jobLocation")))
        .workModel("TELECOMMUTE".equalsIgnoreCase(text(posting, "jobLocationType")) ? "remote" : null)
        .employmentType(employmentType(posting.path("employmentType")))
        .publishedAt(text(posting, "datePosted"))
        .expiresAt(text(posting, "validThrough"))
        .salary(salary(posting.path("baseSalary")))
        .skills(skills(posting.path("skills")))
        .warnings(warnings);
    return builder.build();
  }

  private static String identifier(JsonNode identifier) {
    return identifier.isObject() ? first(text(identifier, "value"), text(identifier, "name")) : text(identifier);
  }

  private static Company company(JsonNode organization) {
    if (organization.isTextual())
      return new Company(blankToNull(organization.asText()), null, null, null, null, Map.of());
    if (!organization.isObject())
      return null;

    Map<String, String> links = new LinkedHashMap<>();
    JsonNode sameAs = organization.path("sameAs");
    List<String> urls = new ArrayList<>();
    if (sameAs.isArray())
      sameAs.forEach(url -> urls.add(text(url)));
    else
      urls.add(text(sameAs));
    for (String url : urls) {
      String valid = httpUrl(url);
      if (valid != null)
        links.putIfAbsent(linkName(valid), valid);
    }

    JsonNode logo = organization.path("logo");
    String logoUrl = logo.isObject() ? first(text(logo, "url"), text(logo, "contentUrl")) : text(logo);

    return new Company(text(organization, "name"), text(organization, "description"),
        httpUrl(text(organization, "url")), httpUrl(logoUrl), null, links);
  }

  private static String linkName(String url) {
    String host = URI.create(url).getHost().toLowerCase(Locale.ROOT);
    String bare = host.startsWith("www.") ? host.substring(4) : host;
    int dot = bare.indexOf('.');
    return dot > 0 ? bare.substring(0, dot) : bare;
  }

  private static Location location(JsonNode jobLocation) {
    JsonNode place = jobLocation.isArray() ? jobLocation.path(0) : jobLocation;
    JsonNode address = place.path("address");
    if (address.isTextual())
      return new Location(null, null, null, blankToNull(address.asText()));
    if (!address.isObject())
      return null;

    JsonNode country = address.path("addressCountry");
    String countryName = country.isObject() ? text(country, "name") : text(country);
    String city = text(address, "addressLocality");
    String state = text(address, "addressRegion");
    String raw = text(address, "streetAddress");
    return city == null && state == null && countryName == null && raw == null
        ? null : new Location(city, state, countryName, raw);
  }

  private static String employmentType(JsonNode type) {
    List<String> labels = new ArrayList<>();
    if (type.isArray())
      type.forEach(item -> labels.add(employmentLabel(text(item))));
    else
      labels.add(employmentLabel(text(type)));
    labels.removeIf(label -> label == null);
    return labels.isEmpty() ? null : String.join(", ", labels);
  }

  private static String employmentLabel(String code) {
    if (code == null)
      return null;
    return switch (code.toUpperCase(Locale.ROOT)) {
      case "FULL_TIME" -> "Tempo integral";
      case "PART_TIME" -> "Meio período";
      case "CONTRACTOR" -> "Contrato (PJ)";
      case "TEMPORARY" -> "Temporário";
      case "INTERN" -> "Estágio";
      case "VOLUNTEER" -> "Voluntário";
      case "PER_DIEM" -> "Diarista";
      default -> code;
    };
  }

  private static Salary salary(JsonNode baseSalary) {
    if (!baseSalary.isObject())
      return null;
    String currency = text(baseSalary, "currency");
    JsonNode value = baseSalary.path("value");
    if (value.isObject()) {
      BigDecimal exact = decimal(value.path("value"));
      BigDecimal min = first(decimal(value.path("minValue")), exact);
      BigDecimal max = first(decimal(value.path("maxValue")), exact);
      String period = text(value, "unitText");
      return min == null && max == null ? null
          : new Salary(min, max, currency, period == null ? null : period.toLowerCase(Locale.ROOT));
    }
    BigDecimal amount = decimal(value);
    return amount == null ? null : new Salary(amount, amount, currency, null);
  }

  private static BigDecimal decimal(JsonNode node) {
    String raw = text(node);
    if (raw == null)
      return null;
    try {
      return new BigDecimal(raw.trim());
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static List<String> skills(JsonNode skills) {
    List<String> names = new ArrayList<>();
    if (skills.isArray()) {
      skills.forEach(skill -> addSkill(names, skill.isObject() ? text(skill, "name") : text(skill)));
    } else {
      String joined = text(skills);
      if (joined != null) {
        for (String part : joined.split("[,;\\n]"))
          addSkill(names, part);
      }
    }
    return names;
  }

  private static void addSkill(List<String> names, String raw) {
    String skill = blankToNull(raw);
    if (skill != null)
      names.add(skill);
  }

  private static List<String> requirementItems(JsonNode requirement) {
    List<String> items = new ArrayList<>();
    if (requirement.isArray()) {
      requirement.forEach(item -> items.addAll(requirementItems(item)));
    } else if (requirement.isObject()) {
      String described = first(text(requirement, "description"), text(requirement, "credentialCategory"));
      if (described != null)
        items.add(described);
    } else {
      String plain = text(requirement);
      if (plain != null)
        items.add(plain);
    }
    return items;
  }

  private static <T> T first(T primary, T fallback) {
    return primary != null ? primary : fallback;
  }
}
