package com.tailorly.api.job;

import static com.tailorly.api.job.JobValues.blankToNull;
import static com.tailorly.api.job.JobValues.httpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingResponse.Company;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** Último recurso para páginas sem dados estruturados: metadados Open Graph e o bloco principal de texto. */
@Component
public class GenericHtmlJobExtractor implements JobPageExtractor {
  private static final int MIN_USEFUL_TEXT = 200;
  private static final String NOISE =
      "script, style, noscript, nav, footer, header, aside, form, iframe, svg, [role=navigation], [role=banner]";
  private static final List<String> CONTENT_SELECTORS = List.of(
      "[itemprop=description]", "[class*=job-description]", "[class*=jobDescription]", "[class*=description]",
      "[id*=description]", "article", "main", "body");

  @Override
  public Optional<JobPostingResponse> extract(Document source, String finalUrl) {
    Document document = source.clone();
    document.select(NOISE).remove();

    JobContentBuilder content = new JobContentBuilder();
    Element container = contentContainer(document);
    if (container != null)
      content.addLines(HtmlText.lines(container), JobSection.OTHER);

    String metaDescription = blankToNull(meta(document, "og:description", "description"));
    if (content.isEmpty() && metaDescription != null)
      content.addPart(null, metaDescription, JobSection.OTHER);

    String title = firstNonNull(
        meta(document, "og:title", "twitter:title"),
        textOf(document.selectFirst("h1")),
        textOf(document.selectFirst("title")));
    if (title == null && content.isEmpty())
      return Optional.empty();

    String siteName = blankToNull(meta(document, "og:site_name", null));
    Company company = siteName == null || siteName.equalsIgnoreCase(title)
        ? null : new Company(siteName, null, null, null, null, null);

    List<String> warnings = new ArrayList<>();
    String body = container == null ? "" : container.text();
    if (body.length() < MIN_USEFUL_TEXT)
      warnings.add("limited_content");

    Element canonical = source.selectFirst("link[rel=canonical]");
    return Optional.of(content.applyTo(JobPostingResponse.builder())
        .platform("generic")
        .canonicalUrl(canonical == null ? null : httpUrl(canonical.absUrl("href")))
        .title(title)
        .company(company)
        .warnings(warnings)
        .build());
  }

  private static Element contentContainer(Document document) {
    for (String selector : CONTENT_SELECTORS) {
      for (Element candidate : document.select(selector)) {
        if (candidate.text().length() >= MIN_USEFUL_TEXT)
          return candidate;
      }
    }
    return document.body();
  }

  private static String meta(Document document, String property, String name) {
    Element element = document.selectFirst("meta[property=" + property + "]");
    if (element == null && name != null)
      element = document.selectFirst("meta[name=" + name + "]");
    return element == null ? null : blankToNull(element.attr("content"));
  }

  private static String textOf(Element element) {
    return element == null ? null : blankToNull(element.text());
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null)
        return value;
    }
    return null;
  }
}
