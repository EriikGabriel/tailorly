package com.tailorly.api.cv.evidence;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import com.tailorly.api.cv.model.JobTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Procura, na conta pública do GitHub da pessoa (o endereço vem dos fatos aprovados), repositórios que
 * mostram o que a vaga pede. Só fala com a API pública do GitHub num host fixo e com o nome de usuário
 * validado, então nenhuma URL vinda de texto de terceiros é buscada. O resultado são <i>candidatos</i>: o
 * que for lido aqui só vira evidência pendente, e o repositório só entra num currículo depois que a pessoa confirma.
 * Um token opcional ({@code tailorly.github.token}) eleva o limite de requisições da API.
 */
@Component
public class GithubRepoScout {
  private static final String API = "https://api.github.com";
  static final int MAX_README_CHARS = 12_000;
  /** A chave da evidência ({@code github:nome}) cabe em 64 caracteres. */
  static final int MAX_NAME = 56;
  private static final Logger log = LoggerFactory.getLogger(GithubRepoScout.class);
  private static final Pattern PROFILE_URL = Pattern.compile(
      "^(?:https?://)?(?:www\\.)?github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))(?:[/?#].*)?$", Pattern.CASE_INSENSITIVE);
  private static final Pattern HANDLE = Pattern.compile("^@?([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))$");
  private static final Pattern REPO_NAME = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");
  private static final Set<String> IGNORED_NAMES = Set.of("dotfiles", ".github", "config", "configs");

  /** Um repositório público; {@code url} é o endereço da página dele. */
  public record Repo(String owner, String name, String url, String description, String language,
      List<String> topics, int stars, boolean fork, boolean archived, int size, Instant pushedAt) {
    /** Metadados em texto, que entram junto com o README no que a IA lê (e no que os trechos podem citar). */
    String metadataText() {
      StringBuilder out = new StringBuilder("Repository: ").append(name).append('\n');
      if (description != null && !description.isBlank()) {
        out.append("Description: ").append(description.strip()).append('\n');
      }
      if (language != null && !language.isBlank()) {
        out.append("Main language: ").append(language).append('\n');
      }
      if (!topics.isEmpty()) {
        out.append("Topics: ").append(String.join(", ", topics)).append('\n');
      }
      if (stars > 0) {
        out.append("Stars: ").append(stars).append('\n');
      }
      return out.toString();
    }
  }

  private final String endpoint;
  private final String token;
  private final RestClient http;

  @Autowired
  public GithubRepoScout(@Value("${tailorly.github.token:}") String token) {
    this(API, token);
  }

  GithubRepoScout(String endpoint, String token) {
    this.endpoint = endpoint;
    this.token = token == null ? "" : token.strip();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  /** O usuário do GitHub citado num fato aprovado ({@code github.com/user}, {@code @user} ou {@code user}). */
  public static Optional<String> usernameOf(String value) {
    if (value == null) {
      return Optional.empty();
    }
    String text = value.strip();
    Matcher url = PROFILE_URL.matcher(text);
    if (url.matches()) {
      return Optional.of(url.group(1));
    }
    Matcher handle = HANDLE.matcher(text);
    return handle.matches() && !text.contains(".") ? Optional.of(handle.group(1)) : Optional.empty();
  }

  /** Repositórios públicos do usuário, dos empurrados mais recentemente aos mais antigos; vazio se a API falhar. */
  public List<Repo> listRepos(String username) {
    if (usernameOf(username).isEmpty()) {
      return List.of();
    }
    try {
      JsonNode body = request("/users/" + username + "/repos?per_page=100&sort=pushed&type=owner")
          .retrieve().body(JsonNode.class);
      List<Repo> repos = new ArrayList<>();
      if (body != null && body.isArray()) {
        for (JsonNode node : body) {
          repoOf(username, node).ifPresent(repos::add);
        }
      }
      return List.copyOf(repos);
    } catch (RuntimeException failure) {
      log.info("GitHub repository listing failed: {}", failure.getClass().getSimpleName());
      return List.of();
    }
  }

  /** O texto que a IA lê de um repositório: metadados e o começo do README. */
  public Optional<String> read(Repo repo) {
    if (!REPO_NAME.matcher(repo.name()).matches() || usernameOf(repo.owner()).isEmpty()) {
      return Optional.empty();
    }
    String readme = "";
    try {
      readme = http.get().uri(endpoint + "/repos/" + repo.owner() + "/" + repo.name() + "/readme")
          .headers(headers -> {
            headers.set(HttpHeaders.ACCEPT, "application/vnd.github.raw+json");
            headers.set(HttpHeaders.USER_AGENT, "tailorly");
            if (!token.isEmpty()) {
              headers.setBearerAuth(token);
            }
          }).retrieve().body(String.class);
    } catch (RuntimeException failure) {
      log.info("GitHub README read failed: {}", failure.getClass().getSimpleName());
    }
    if (readme == null) {
      readme = "";
    }
    if (readme.length() > MAX_README_CHARS) {
      readme = readme.substring(0, MAX_README_CHARS);
    }
    String text = (repo.metadataText() + "\n" + readme).strip();
    // Sem README e sem descrição não há o que a pessoa possa confirmar.
    boolean hasContent = !readme.isBlank() || (repo.description() != null && !repo.description().isBlank());
    return hasContent ? Optional.of(text) : Optional.empty();
  }

  private RestClient.RequestHeadersSpec<?> request(String path) {
    return http.get().uri(endpoint + path).headers(headers -> {
      headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
      headers.set(HttpHeaders.USER_AGENT, "tailorly");
      headers.set("X-GitHub-Api-Version", "2022-11-28");
      if (!token.isEmpty()) {
        headers.setBearerAuth(token);
      }
    });
  }

  private static Optional<Repo> repoOf(String username, JsonNode node) {
    String name = node.path("name").asText("");
    if (!REPO_NAME.matcher(name).matches()) {
      return Optional.empty();
    }
    List<String> topics = new ArrayList<>();
    node.path("topics").forEach(topic -> topics.add(topic.asText("")));
    Instant pushed = null;
    try {
      String value = node.path("pushed_at").asText("");
      pushed = value.isEmpty() ? null : Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      // sem data, o repositório só perde o ponto de recência
    }
    return Optional.of(new Repo(username, name, node.path("html_url").asText("https://github.com/" + username + "/" + name),
        node.path("description").isNull() ? null : node.path("description").asText(null),
        node.path("language").isNull() ? null : node.path("language").asText(null), List.copyOf(topics),
        node.path("stargazers_count").asInt(0), node.path("fork").asBoolean(false),
        node.path("archived").asBoolean(false), node.path("size").asInt(0), pushed));
  }

  /**
   * Os {@code limit} repositórios que mais combinam com a vaga: não são forks nem arquivados, não estão no
   * Currículo Base ({@code knownNames}, em minúsculas) e não são o perfil ou repositórios de configuração.
   * Pesa mais quem tem termos da vaga no nome, na descrição, nos tópicos e na linguagem; estrelas e
   * atividade recente desempatam e favorecem projetos mais completos.
   */
  static List<Repo> rank(List<Repo> repos, JobTerms terms, Set<String> knownNames, String username, int limit) {
    Instant recent = Instant.now().minus(Duration.ofDays(730));
    return repos.stream()
        .filter(repo -> !repo.fork() && !repo.archived() && repo.size() > 0 && repo.name().length() <= MAX_NAME)
        .filter(repo -> !repo.name().equalsIgnoreCase(username)
            && !IGNORED_NAMES.contains(repo.name().toLowerCase(Locale.ROOT))
            && !knownNames.contains(repo.name().toLowerCase(Locale.ROOT)))
        .sorted(Comparator.comparingInt((Repo repo) -> -score(repo, terms, recent)))
        .limit(limit).collect(Collectors.toList());
  }

  private static int score(Repo repo, JobTerms terms, Instant recent) {
    String text = String.join(" ", repo.name().replace('-', ' ').replace('_', ' '),
        repo.description() == null ? "" : repo.description(), repo.language() == null ? "" : repo.language(),
        String.join(" ", repo.topics()));
    int score = terms.matchedIn(text).size() * 6;
    score += Math.min(repo.stars(), 20) / 7;
    if (repo.pushedAt() != null && repo.pushedAt().isAfter(recent)) {
      score += 2;
    }
    if (repo.description() != null && !repo.description().isBlank()) {
      score += 1;
    }
    return score;
  }
}
