package com.tailorly.api.job.reader;

import static com.tailorly.api.job.reader.JobValues.blankToNull;
import static com.tailorly.api.job.reader.JobValues.httpUrl;
import static com.tailorly.api.job.reader.JobValues.text;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingResponse.Company;
import com.tailorly.api.job.dto.JobPostingResponse.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Lê vagas da InHire ({@code {empresa}.inhire.app/vagas/{id}/{slug}}). A página é uma SPA sem conteúdo
 * estático, então o reconhecimento é pelo endereço e os dados vêm da API pública que a própria página
 * consome. Não depende do HTML, por isso é usado no lugar do download da página.
 */
@Component
@RequiredArgsConstructor
public class InhireJobExtractor {
  private static final Pattern HOST = Pattern.compile("^([a-z0-9](?:[a-z0-9-]{0,62}))\\.inhire\\.app$");
  private static final Pattern PATH = Pattern.compile(
      "^/vagas/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?:/.*)?$");
  private static final java.util.Set<String> RESERVED_TENANTS = java.util.Set.of("api", "www", "files", "app");

  private final InhireJobClient client;

  /** Empresa (subdomínio) e ID da vaga extraídos do link. */
  record Target(String tenant, String jobId) {
  }

  static Optional<Target> match(String url) {
    try {
      URI uri = URI.create(url.trim());
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      if (!scheme.equals("https") && !scheme.equals("http"))
        return Optional.empty();
      if (uri.getHost() == null || uri.getPath() == null || uri.getUserInfo() != null)
        return Optional.empty();
      Matcher host = HOST.matcher(uri.getHost().toLowerCase(Locale.ROOT));
      Matcher path = PATH.matcher(uri.getPath());
      if (!host.matches() || !path.matches() || RESERVED_TENANTS.contains(host.group(1)))
        return Optional.empty();
      return Optional.of(new Target(host.group(1), path.group(1).toLowerCase(Locale.ROOT)));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /** Vazio quando o link não é da InHire ou a vaga não existe mais. */
  public Optional<JobPostingResponse> extract(String url) {
    Optional<Target> target = match(url);
    if (target.isEmpty())
      return Optional.empty();
    return client.fetch(target.get().tenant(), target.get().jobId())
        .flatMap(job -> parse(job, target.get()));
  }

  private static Optional<JobPostingResponse> parse(JsonNode job, Target target) {
    String title = text(job, "displayName");
    if (title == null)
      return Optional.empty();

    JobContentBuilder content = new JobContentBuilder();
    content.addPart(null, text(job, "description"), JobSection.OTHER);

    List<String> warnings = new ArrayList<>();
    String status = text(job, "status");
    if (status != null && !status.equals("published"))
      warnings.add("job_not_published");

    String origin = "https://" + target.tenant() + ".inhire.app";
    return Optional.of(content.applyTo(JobPostingResponse.builder())
        .platform("inhire")
        .canonicalUrl(origin + "/vagas/" + target.jobId())
        .externalId(text(job, "jobId") != null ? text(job, "jobId") : target.jobId())
        .title(title)
        .status(status)
        .company(company(job, origin))
        .location(location(text(job, "location")))
        .workModel(JobValues.normalizeWorkModel(text(job, "workplaceType")))
        .employmentType(employmentType(job.path("contractType")))
        .publishedAt(text(job, "publishedAt"))
        .warnings(warnings)
        .build());
  }

  private static Company company(JsonNode job, String careerPageUrl) {
    String name = text(job, "tenantName");
    List<HtmlText.Line> about = HtmlText.fromMarkupOrText(text(job, "about"));
    if (name == null && about.isEmpty())
      return null;
    return new Company(name, about.isEmpty() ? null : HtmlText.join(about), null,
        httpUrl(text(job, "logo")), careerPageUrl, java.util.Map.of());
  }

  private static Location location(String raw) {
    return raw == null ? null : new Location(null, null, null, raw);
  }

  private static String employmentType(JsonNode contractType) {
    if (!contractType.isArray() || contractType.isEmpty())
      return null;
    List<String> types = new ArrayList<>();
    contractType.forEach(type -> {
      String value = blankToNull(type.asText(null));
      if (value != null)
        types.add(value);
    });
    return types.isEmpty() ? null : String.join(", ", types);
  }
}
