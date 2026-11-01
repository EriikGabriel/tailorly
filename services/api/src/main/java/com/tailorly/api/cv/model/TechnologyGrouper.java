package com.tailorly.api.cv.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.tailorly.api.cv.model.CvDocument.Entry;
import com.tailorly.api.cv.model.CvDocument.Section;

/**
 * Agrupa as tecnologias aprovadas por categoria ("Backend: Java, Spring Boot"), em vez de uma lista
 * de itens soltos que ocupa a página inteira. O agrupamento é só apresentação: cada tecnologia
 * aparece exatamente uma vez, com o nome aprovado, e nenhuma é criada ou removida. Nomes fora do
 * dicionário vão para "Outras", então uma tecnologia desconhecida nunca é descartada nem renomeada.
 */
public final class TechnologyGrouper {
  static final String OTHERS = "Outras";
  private static final int MIN_TECHNOLOGIES = 5;

  private static final Map<String, List<String>> CATALOG = new LinkedHashMap<>();

  static {
    CATALOG.put("Linguagens", List.of("java", "kotlin", "python", "javascript", "typescript", "c", "c++", "c#",
        "go", "golang", "rust", "php", "ruby", "swift", "dart", "scala", "sql", "bash", "shell", "r"));
    CATALOG.put("Backend", List.of("spring", "spring boot", "spring cloud", "spring security", "spring data",
        "hibernate", "jpa", "node", "node.js", "nodejs", "express", "nestjs", "fastify", "flask", "django",
        "fastapi", ".net", "asp.net", "laravel", "rails", "ruby on rails", "rest", "restful", "api rest",
        "graphql", "grpc", "microsservicos", "microservices", "rabbitmq", "kafka", "apache kafka"));
    CATALOG.put("Frontend", List.of("react", "react.js", "reactjs", "next", "next.js", "nextjs", "vue",
        "vue.js", "angular", "svelte", "html", "html5", "css", "css3", "sass", "scss", "tailwind",
        "tailwind css", "bootstrap", "redux", "vite", "webpack", "jquery", "shadcn", "tanstack query"));
    CATALOG.put("Mobile", List.of("flutter", "react native", "android", "ios", "expo", "swiftui",
        "jetpack compose"));
    CATALOG.put("Banco de dados", List.of("postgresql", "postgres", "mysql", "mariadb", "sql server",
        "oracle", "mongodb", "redis", "sqlite", "dynamodb", "cassandra", "elasticsearch", "firebase",
        "supabase", "flyway", "liquibase"));
    CATALOG.put("Cloud e DevOps", List.of("docker", "docker compose", "kubernetes", "k8s", "aws", "azure",
        "gcp", "google cloud", "terraform", "ansible", "ci/cd", "github actions", "gitlab ci", "jenkins",
        "nginx", "linux", "vercel", "heroku", "cloudflare"));
    CATALOG.put("Testes", List.of("junit", "mockito", "jest", "vitest", "cypress", "playwright", "selenium",
        "testcontainers", "tdd", "pytest", "testing library"));
    CATALOG.put("Dados e IA", List.of("pandas", "numpy", "scikit-learn", "tensorflow", "pytorch", "spark",
        "airflow", "power bi", "tableau", "machine learning", "llm", "langchain", "ocr"));
    CATALOG.put("Ferramentas", List.of("git", "github", "gitlab", "bitbucket", "jira", "postman", "swagger",
        "openapi", "figma", "intellij", "vscode", "maven", "gradle", "npm", "pnpm", "yarn", "scrum", "kanban",
        "agile", "metodologias ageis"));
  }

  private TechnologyGrouper() {
  }

  /** Devolve o documento com a seção de tecnologias agrupada, ou o próprio documento se não compensar. */
  public static CvDocument apply(CvDocument document) {
    List<Section> sections = new ArrayList<>();
    boolean changed = false;
    for (Section section : document.sections()) {
      Section grouped = "technologies".equals(section.key()) ? group(section) : section;
      changed |= grouped != section;
      sections.add(grouped);
    }
    if (!changed) {
      return document;
    }
    return new CvDocument(document.name(), document.headline(), document.summary(), document.contacts(),
        List.copyOf(sections), document.layout());
  }

  private static Section group(Section section) {
    if (section.grouped() || section.entries().size() < MIN_TECHNOLOGIES) {
      return section;
    }
    // LinkedHashMap: os grupos seguem a ordem do primeiro item, que já vem ordenado por relevância à vaga.
    Map<String, List<String>> groups = new LinkedHashMap<>();
    for (Entry entry : section.entries()) {
      groups.computeIfAbsent(category(entry.title()), label -> new ArrayList<>()).add(label(entry));
    }
    // Sem nenhum grupo com mais de um item, agrupar só trocaria cada item por "Rótulo: item".
    if (groups.size() >= section.entries().size()) {
      return section;
    }
    List<Entry> entries = new ArrayList<>();
    List<String> others = groups.remove(OTHERS);
    groups.forEach((label, items) -> entries.add(groupEntry(label, items)));
    if (others != null) {
      entries.add(groupEntry(OTHERS, others));
    }
    return new Section(section.key(), section.heading(), false, true, List.copyOf(entries));
  }

  private static Entry groupEntry(String label, List<String> items) {
    return new Entry("technologies:" + slug(label), label, null, null, null,
        List.of(String.join(", ", items)), null);
  }

  private static String label(Entry entry) {
    String context = entry.subtitle();
    return context == null || context.isBlank() ? entry.title() : entry.title() + " (" + context.strip() + ")";
  }

  static String category(String name) {
    String key = normalize(name);
    for (Map.Entry<String, List<String>> group : CATALOG.entrySet()) {
      if (group.getValue().contains(key)) {
        return group.getKey();
      }
    }
    return OTHERS;
  }

  private static String slug(String label) {
    return normalize(label).replaceAll("[^a-z0-9]+", "-");
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
  }
}
