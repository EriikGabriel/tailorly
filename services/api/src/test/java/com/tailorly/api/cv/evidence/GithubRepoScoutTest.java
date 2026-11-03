package com.tailorly.api.cv.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.junit.jupiter.api.Test;

class GithubRepoScoutTest {
  private static GithubRepoScout.Repo repo(String name, String description, String language, int stars,
      boolean fork) {
    return new GithubRepoScout.Repo("ana", name, "https://github.com/ana/" + name, description, language,
        List.of(), stars, fork, false, 100, Instant.now());
  }

  @Test
  void readsTheUsernameFromProfileUrlsAndHandles() {
    assertEquals(Optional.of("EriikGabriel"), GithubRepoScout.usernameOf("github.com/EriikGabriel"));
    assertEquals(Optional.of("EriikGabriel"), GithubRepoScout.usernameOf("https://github.com/EriikGabriel/"));
    assertEquals(Optional.of("ana-dev"), GithubRepoScout.usernameOf("@ana-dev"));
    assertEquals(Optional.of("ana"), GithubRepoScout.usernameOf("https://www.github.com/ana/repo"));
    assertTrue(GithubRepoScout.usernameOf("https://evil.example/ana").isEmpty());
    assertTrue(GithubRepoScout.usernameOf("ana/../../x").isEmpty());
    assertTrue(GithubRepoScout.usernameOf("a.b").isEmpty());
    assertTrue(GithubRepoScout.usernameOf(null).isEmpty());
  }

  @Test
  void ranksByJobFitSkippingForksKnownProjectsAndProfileRepos() {
    JobTerms terms = JobTerms.of(JobPostingResponse.builder().skills(List.of("java", "docker")).build(), null);
    List<GithubRepoScout.Repo> ranked = GithubRepoScout.rank(List.of(
        repo("ana", "perfil", null, 99, false),
        repo("fork-java", "Java", "Java", 50, true),
        repo("loja", "App de loja", "PHP", 30, false),
        repo("api-java", "API em Java com Docker", "Java", 2, false),
        repo("ja-no-base", "Java", "Java", 10, false)),
        terms, Set.of("ja-no-base"), "ana", 5);

    assertEquals(List.of("api-java", "loja"), ranked.stream().map(GithubRepoScout.Repo::name).toList());
  }

  @Test
  void limitsHowManyRepositoriesAreReturned() {
    List<GithubRepoScout.Repo> ranked = GithubRepoScout.rank(List.of(
        repo("a", "x", null, 1, false), repo("b", "x", null, 2, false), repo("c", "x", null, 3, false)),
        JobTerms.of(null, null), Set.of(), "ana", 2);
    assertEquals(2, ranked.size());
  }
}
