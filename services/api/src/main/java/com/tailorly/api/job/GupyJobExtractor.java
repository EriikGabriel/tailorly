package com.tailorly.api.job;

import static com.tailorly.api.job.JobValues.blankToNull;
import static com.tailorly.api.job.JobValues.httpUrl;
import static com.tailorly.api.job.JobValues.text;

import java.util.ArrayList;
import java.util.Comparator;
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
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Lê vagas da Gupy a partir do JSON que a página Next.js embute em {@code __NEXT_DATA__}
 * ({@code props.pageProps.job}). O reconhecimento é pelo conteúdo, não pelo domínio, para que
 * páginas de carreira com domínio próprio hospedadas na Gupy também funcionem.
 */
@Component
@RequiredArgsConstructor
public class GupyJobExtractor implements JobPageExtractor {
  private final ObjectMapper objectMapper;

  @Override
  public Optional<JobPostingResponse> extract(Document document, String finalUrl) {
    Element script = document.selectFirst("script#__NEXT_DATA__");
    if (script == null)
      return Optional.empty();

    JsonNode pageProps;
    try {
      pageProps = objectMapper.readTree(script.data()).path("props").path("pageProps");
    } catch (JsonProcessingException exception) {
      return Optional.empty();
    }

    JsonNode job = pageProps.path("job");
    if (!job.isObject() || text(job, "name") == null || !(job.has("careerPage") || job.has("jobSteps")))
      return Optional.empty();

    JobContentBuilder content = new JobContentBuilder();
    content.addPart("Descrição", text(job, "description"), JobSection.OTHER);
    content.addPart("Responsabilidades e atribuições", text(job, "responsibilities"), JobSection.RESPONSIBILITIES);
    content.addPart("Requisitos e qualificações", text(job, "prerequisites"), JobSection.REQUIREMENTS);
    content.addPart("Informações adicionais", text(job, "relevantExperiences"), JobSection.OTHER);

    List<String> warnings = new ArrayList<>();
    String status = text(job, "status");
    if (status != null && !status.equals("published"))
      warnings.add("job_not_published");

    JobPostingResponse.JobPostingResponseBuilder builder = content.applyTo(JobPostingResponse.builder())
        .platform("gupy")
        .canonicalUrl(httpUrl(text(pageProps, "canonicalUrl")))
        .externalId(text(job, "id"))
        .title(text(job, "name"))
        .status(status)
        .company(company(job.path("careerPage"), job.path("company")))
        .location(location(job))
        .workModel(JobValues.normalizeWorkModel(text(job, "workplaceType")))
        .employmentType(employmentType(text(job, "jobType")))
        .publishedAt(text(job, "publishedAt"))
        .expiresAt(first(text(job, "registerEndDate"), text(job, "expiresAt")))
        .skills(skills(job.path("skills")))
        .warnings(warnings);

    // As etapas do seletivo vêm estruturadas e prevalecem sobre as inferidas do texto.
    List<String> steps = steps(job.path("jobSteps"));
    if (!steps.isEmpty())
      builder.hiringProcess(steps);
    return Optional.of(builder.build());
  }

  private static Company company(JsonNode careerPage, JsonNode companyNode) {
    if (!careerPage.isObject())
      return null;

    // O nome da página de carreira costuma trazer um slogan depois de " | ".
    String rawName = text(careerPage, "name");
    String name = rawName == null ? null : blankToNull(rawName.split("\\s\\|\\s", 2)[0]);

    JsonNode social = careerPage.path("socialLinks");
    Map<String, String> links = new LinkedHashMap<>();
    social.fieldNames().forEachRemaining(field -> {
      String url = httpUrl(text(social, field));
      if (url != null && field.startsWith("url") && !field.equals("urlSite"))
        links.put(field.substring(3).toLowerCase(Locale.ROOT), url);
    });

    String subdomain = first(text(careerPage, "subdomain"), text(companyNode, "subdomain"));
    List<HtmlText.Line> about = HtmlText.fromMarkupOrText(text(careerPage, "about"));
    String logo = first(text(careerPage, "urlLogo"), text(careerPage.path("appearances"), "companyLogoUrl"));

    return new Company(
        name,
        about.isEmpty() ? null : HtmlText.join(about),
        httpUrl(first(text(social, "urlSite"), text(careerPage, "urlSite"))),
        httpUrl(logo),
        subdomain == null || !subdomain.matches("[A-Za-z0-9-]+") ? null : "https://" + subdomain + ".gupy.io",
        links);
  }

  private static Location location(JsonNode job) {
    String city = text(job, "addressCity");
    String state = first(text(job, "addressStateShortName"), text(job, "addressState"));
    String country = text(job, "addressCountry");
    String raw = text(job, "addressLine");
    return city == null && state == null && country == null && raw == null
        ? null : new Location(city, state, country, raw);
  }

  private static String employmentType(String code) {
    if (code == null)
      return null;
    return switch (code) {
      case "vacancy_type_effective" -> "Efetivo";
      case "vacancy_legal_entity" -> "PJ";
      case "vacancy_type_intern" -> "Estágio";
      case "vacancy_type_apprentice" -> "Aprendiz";
      case "vacancy_type_temporary" -> "Temporário";
      case "vacancy_type_talent_pool" -> "Banco de talentos";
      default -> code;
    };
  }

  private static List<String> steps(JsonNode steps) {
    if (!steps.isArray())
      return List.of();
    List<JsonNode> ordered = new ArrayList<>();
    steps.forEach(ordered::add);
    ordered.sort(Comparator.comparingInt(step -> step.path("order").asInt(Integer.MAX_VALUE)));
    List<String> names = new ArrayList<>();
    for (JsonNode step : ordered) {
      String name = text(step, "name");
      if (name != null)
        names.add(name);
    }
    return names;
  }

  private static List<String> skills(JsonNode skills) {
    if (!skills.isArray())
      return List.of();
    List<String> names = new ArrayList<>();
    for (JsonNode skill : skills) {
      String name = skill.isObject() ? text(skill, "name") : text(skill);
      if (name != null)
        names.add(name);
    }
    return names;
  }

  private static String first(String primary, String fallback) {
    return primary != null ? primary : fallback;
  }
}
