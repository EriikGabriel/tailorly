package com.tailorly.api.cv.evidence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.base.snapshot.GtSnapshotItem;
import com.tailorly.api.base.snapshot.GtSnapshotItemRepository;
import com.tailorly.api.base.snapshot.GtSnapshotRepository;
import com.tailorly.api.cv.enrichment.ConfirmedEvidence;
import com.tailorly.api.cv.evidence.dto.SupplementaryEvidenceResponse;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.job.reader.FetchedPage;
import com.tailorly.api.job.reader.JobPageFetcher;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.search.WebSearchClient;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import com.tailorly.api.shared.util.Text;
import com.tailorly.api.vault.OwnerVaultService;
import com.tailorly.api.vault.VaultNoteReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coleta evidências complementares de três fontes sobre os itens do snapshot ({@link EvidenceSubjects}:
 * projetos, com ou sem link, e experiências):
 * <ul>
 *   <li><b>base_link</b>: os links já presentes nos fatos aprovados;</li>
 *   <li><b>web</b>: páginas encontradas por busca sobre o projeto (se houver provedor configurado); só
 *       valem as que citam o projeto pelo nome;</li>
 *   <li><b>vault</b>: as notas do Obsidian que mais citam o item (título, repositório ou empresa), se a
 *       leitura estiver habilitada; só as passagens em torno da citação vão à IA.</li>
 * </ul>
 * Além dos itens do snapshot, contas com GitHub nos fatos aprovados e a busca web ligada também têm
 * <b>repositórios candidatos</b> ({@code github}): os repositórios públicos que mais combinam com a vaga
 * informada e que não estão no Currículo Base viram evidências pendentes de um projeto novo (chave
 * {@code github:repo}), que só entra num currículo depois de confirmado.
 * Não altera o snapshot nem os fatos: cada detalhe vira uma evidência {@code pendente}.
 *
 * <p>Custo controlado: sem provedor de IA não há coleta; no máximo {@link #MAX_ANALYSES} análises de IA
 * por chamada; no máximo {@link #MAX_SEARCH_PROJECTS} buscas por chamada; cada análise consome uma
 * unidade do limite de processamento do visitante (contas não têm limite). Cada fonte é marcada como
 * analisada (inclusive buscas e páginas sem relação com o projeto), e fontes já marcadas não são
 * buscadas nem analisadas de novo. Rede e IA rodam fora de transação.</p>
 */
@Component
@RequiredArgsConstructor
public class SupplementaryEvidenceService {
  /** Visitantes têm o mesmo teto de antes; contas, que não têm cota de processamento, podem ir mais fundo. */
  static final int MAX_ANALYSES = 5;
  static final int MAX_ANALYSES_ACCOUNT = 12;
  static final int MAX_SEARCH_PROJECTS = 3;
  static final int MAX_SEARCH_PROJECTS_ACCOUNT = 6;
  static final int MAX_NOTES_PER_SUBJECT = 3;
  static final int SEARCH_RESULTS = 3;
  static final int MAX_WEB_PAGES_PER_PROJECT = 2;
  static final int MAX_GITHUB_REPOS = 3;
  private static final Logger log = LoggerFactory.getLogger(SupplementaryEvidenceService.class);
  private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+");

  private final GtSnapshotRepository snapshots;
  private final GtSnapshotItemRepository snapshotItems;
  private final OwnerAccessService access;
  private final SupplementaryEvidenceRepository evidence;
  private final JobPageFetcher pageFetcher;
  private final BaseLinkEvidenceExtractor extractor;
  private final AiPreferencesService preferences;
  private final AnonymousQuotaService quota;
  private final WebSearchClient webSearch;
  private final GithubRepoScout github;
  private final JobPostingRepository postings;
  private final JobPostingVersionRepository jobVersions;
  private final VaultNoteReader vault;
  private final OwnerVaultService ownerVaults;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  /**
   * @param linksFound        links distintos encontrados nos fatos aprovados
   * @param linksAlreadyKnown links que já foram analisados e foram ignorados
   * @param linksFailed       fontes que não puderam ser baixadas ou analisadas nesta chamada
   * @param evidenceCreated   evidências pendentes criadas nesta chamada
   * @param aiStatus          {@code completed}, {@code limit_reached} (limite de processamento atingido no
   *                          meio da coleta) ou {@code unavailable} (provedor {@code rules}, sem coleta)
   */
  public record CollectionResult(int linksFound, int linksAlreadyKnown, int linksFailed, int evidenceCreated,
      String aiStatus) {
  }

  private record Plan(UUID ownerId, int linksFound, int alreadyKnown, List<String> pendingLinks,
      List<EvidenceSubject> subjects, Set<String> analyzed, boolean aiAvailable, Optional<Path> vaultDir,
      boolean webEnabled, boolean account, Optional<String> githubUser, JobTerms terms,
      Set<String> knownProjectNames) {
  }

  /** Contador de uma chamada: análises de IA restantes e falhas acumuladas. */
  private static final class Run {
    int budget;
    int created;
    int failed;
    String aiStatus = "completed";

    Run(int budget) {
      this.budget = budget;
    }
  }

  public CollectionResult collect(UUID snapshotId) {
    return collect(snapshotId, null);
  }

  /**
   * @param jobPostingId vaga que orienta a busca de repositórios candidatos; sem ela, os repositórios são
   *                     ordenados só por estrelas e atividade recente
   */
  public CollectionResult collect(UUID snapshotId, UUID jobPostingId) {
    Plan plan = transaction.execute(status -> plan(snapshotId, jobPostingId));
    if (!plan.aiAvailable()) {
      return new CollectionResult(plan.linksFound(), plan.alreadyKnown(), 0, 0, "unavailable");
    }

    Run run = new Run(plan.account() ? MAX_ANALYSES_ACCOUNT : MAX_ANALYSES);
    try {
      for (String link : plan.pendingLinks()) {
        if (run.budget == 0) {
          break;
        }
        EvidenceSubject subject = plan.subjects().stream().filter(item -> link.equals(item.link())).findFirst()
            .orElse(null);
        attempt(run, () -> analyzePage(snapshotId, plan.ownerId(), "base_link", link, subject, link));
      }
      collectSubjectSources(snapshotId, plan, run);
      collectGithubProjects(snapshotId, plan, run);
    } catch (TooManyRequestsException limit) {
      run.aiStatus = "limit_reached";
    }
    return new CollectionResult(plan.linksFound(), plan.alreadyKnown(), run.failed, run.created, run.aiStatus);
  }

  /**
   * Busca web (só projetos) e leitura do vault (projetos e experiências) para cada item, dentro do
   * orçamento da chamada. Itens sem link também entram: a evidência é casada pela chave do item.
   */
  private void collectSubjectSources(UUID snapshotId, Plan plan, Run run) {
    int searchLimit = plan.account() ? MAX_SEARCH_PROJECTS_ACCOUNT : MAX_SEARCH_PROJECTS;
    int searched = 0;
    for (EvidenceSubject subject : plan.subjects()) {
      if (plan.webEnabled() && subject.searchable() && searched < searchLimit && run.budget > 0) {
        String searchKey = "search:" + subject.key();
        if (!plan.analyzed().contains(searchKey)) {
          searched++;
          List<WebSearchClient.SearchResult> results = searchOrEmpty(subject.searchQuery());
          markChecked(snapshotId, plan.ownerId(), searchKey, results.size());
          int pages = 0;
          for (WebSearchClient.SearchResult result : results) {
            if (pages == MAX_WEB_PAGES_PER_PROJECT || run.budget == 0) {
              break;
            }
            if (plan.analyzed().contains(result.url())) {
              continue;
            }
            pages++;
            attempt(run, () -> analyzeWebResult(snapshotId, plan.ownerId(), subject, result.url()));
          }
        }
      }
      if (plan.vaultDir().isPresent()) {
        for (VaultNoteReader.Note note : vault.findMentioningAny(plan.vaultDir().get(), subject.aliases(),
            MAX_NOTES_PER_SUBJECT)) {
          // A mesma nota pode servir a itens diferentes (projeto e empresa): a chave inclui o item.
          String key = "vault:" + subject.key() + ":" + note.key();
          if (plan.analyzed().contains(key) || run.budget == 0) {
            continue;
          }
          attempt(run, () -> analyzeText(snapshotId, plan.ownerId(), "vault", null, subject, key, note.text()));
        }
      }
    }
  }

  /**
   * Repositórios públicos do GitHub da pessoa que combinam com a vaga e ainda não estão no Currículo Base.
   * Cada um é analisado uma vez por snapshot (a chave fica em {@code cv_evidence_link_checks}), mesmo sem
   * detalhes, e consome o orçamento de análises da chamada. Só contas, com a busca web ligada.
   */
  private void collectGithubProjects(UUID snapshotId, Plan plan, Run run) {
    if (plan.githubUser().isEmpty() || !plan.account() || run.budget == 0) {
      return;
    }
    String user = plan.githubUser().get();
    List<GithubRepoScout.Repo> candidates = GithubRepoScout.rank(github.listRepos(user), plan.terms(),
        plan.knownProjectNames(), user, MAX_GITHUB_REPOS * 3).stream()
        .filter(repo -> !plan.analyzed().contains("github:" + user + "/" + repo.name()))
        .limit(MAX_GITHUB_REPOS).toList();
    for (GithubRepoScout.Repo repo : candidates) {
      if (run.budget == 0) {
        break;
      }
      String checkKey = "github:" + user + "/" + repo.name();
      Optional<String> text = github.read(repo);
      if (text.isEmpty()) {
        markChecked(snapshotId, plan.ownerId(), checkKey, 0);
        continue;
      }
      EvidenceSubject subject = new EvidenceSubject(ConfirmedEvidence.CANDIDATE_PREFIX + repo.name(),
          EvidenceSubject.Kind.PROJECT, repo.name(), List.of(repo.name()), repo.url(), null);
      attempt(run, () -> analyzeText(snapshotId, plan.ownerId(), "github", repo.url(), subject, checkKey, text.get()));
    }
  }

  /** Executa uma análise de IA, contabiliza o orçamento e transforma falhas isoladas em contagem. */
  private void attempt(Run run, Analysis analysis) {
    if (run.budget == 0) {
      return;
    }
    try {
      run.created += analysis.run();
      run.budget--;
    } catch (TooManyRequestsException limit) {
      throw limit;
    } catch (RuntimeException failure) {
      // Uma fonte ruim não derruba a coleta das demais; o motivo já foi registrado por quem falhou.
      run.failed++;
      log.info("Supplementary evidence skipped for one source: {}", failure.getClass().getSimpleName());
    }
  }

  @FunctionalInterface
  private interface Analysis {
    int run();
  }

  private List<WebSearchClient.SearchResult> searchOrEmpty(String title) {
    try {
      return webSearch.search(title, SEARCH_RESULTS);
    } catch (RuntimeException failure) {
      log.info("Web search failed: {}", failure.getClass().getSimpleName());
      return List.of();
    }
  }

  /** Página de link do Currículo Base: sempre analisada se baixada (o link é do próprio projeto, quando há). */
  private int analyzePage(UUID snapshotId, UUID ownerId, String kind, String url, EvidenceSubject subject,
      String key) {
    FetchedPage page = pageFetcher.fetch(url);
    return analyzeText(snapshotId, ownerId, kind, url, subject, key, pageText(page));
  }

  /** Página encontrada pela busca: só conta se citar o item pelo nome; senão é marcada e descartada. */
  private int analyzeWebResult(UUID snapshotId, UUID ownerId, EvidenceSubject subject, String url) {
    FetchedPage page = pageFetcher.fetch(url);
    String text = pageText(page);
    if (subject.aliases().stream().noneMatch(alias -> Text.mentions(text, alias))) {
      markChecked(snapshotId, ownerId, url, 0);
      return 0;
    }
    return analyzeText(snapshotId, ownerId, "web", url, subject, url, text);
  }

  private int analyzeText(UUID snapshotId, UUID ownerId, String kind, String sourceUrl, EvidenceSubject subject,
      String checkKey, String text) {
    List<BaseLinkEvidenceExtractor.Claim> claims = extractor.extract(ownerId, text, subject);
    return save(snapshotId, kind, sourceUrl, subject, checkKey, claims);
  }

  private static String pageText(FetchedPage page) {
    return page.document().body() == null ? page.document().text() : page.document().body().text();
  }

  private Plan plan(UUID snapshotId, UUID jobPostingId) {
    GtSnapshot snapshot = loadAuthorized(snapshotId);
    UUID ownerId = snapshot.getOwner().getId();

    List<String> links = baseLinks(snapshot.getId());
    Set<String> analyzed = new HashSet<>(jdbc.queryForList(
        "select source_url from cv_evidence_link_checks where snapshot_id = ?", String.class, snapshot.getId()));
    List<String> pending = links.stream().filter(link -> !analyzed.contains(link)).toList();
    int alreadyKnown = (int) links.stream().filter(analyzed::contains).count();

    boolean aiAvailable = !"rules".equals(preferences.resolveEffectiveSettings(ownerId).provider());
    List<GtSnapshotItem> items = snapshotItems.findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(snapshot.getId());
    List<EvidenceSubject> subjects = EvidenceSubjects.of(items);
    boolean account = !AnonymousQuotaService.isAnonymous(snapshot.getOwner());
    boolean webEnabled = ownerVaults.webSearchEnabledFor(ownerId);
    Optional<String> githubUser = account && aiAvailable && webEnabled ? githubUserOf(items) : Optional.empty();
    Set<String> knownNames = new HashSet<>();
    subjects.stream().filter(subject -> subject.kind() == EvidenceSubject.Kind.PROJECT).forEach(
        subject -> subject.aliases().forEach(alias -> knownNames.add(alias.toLowerCase(java.util.Locale.ROOT))));
    return new Plan(ownerId, links.size(), alreadyKnown, aiAvailable ? pending : List.of(), subjects,
        analyzed, aiAvailable, ownerVaults.readableVaultOf(ownerId), webSearch.isConfigured() && webEnabled,
        account, githubUser, githubUser.isPresent() ? jobTermsOf(snapshot, jobPostingId) : JobTerms.of(null, null),
        knownNames);
  }

  /** Termos da vaga informada, se ela é do mesmo owner do snapshot; senão, nenhum (ordena por estrelas). */
  private JobTerms jobTermsOf(GtSnapshot snapshot, UUID jobPostingId) {
    if (jobPostingId == null) {
      return JobTerms.of(null, null);
    }
    Optional<JobPosting> posting = postings.findById(jobPostingId)
        .filter(item -> item.getOwner().getId().equals(snapshot.getOwner().getId()));
    Optional<JobPostingVersion> version = posting.flatMap(item ->
        jobVersions.findByJobPostingIdAndVersionNumber(item.getId(), item.getLatestVersionNumber()));
    return version.map(item -> JobTerms.of(item.getDetails(), item.getAnalysis()))
        .orElseGet(() -> JobTerms.of(null, null));
  }

  private static Optional<String> githubUserOf(List<GtSnapshotItem> items) {
    return items.stream()
        .filter(item -> "identity".equals(item.getCategory()) && "github".equals(item.getFieldKey())
            && item.getApprovedValue() instanceof String)
        .map(item -> GithubRepoScout.usernameOf((String) item.getApprovedValue())).flatMap(Optional::stream)
        .findFirst();
  }

  /**
   * Grava os detalhes de uma fonte e a marca como analisada, na mesma transação. O limite de
   * processamento é consumido aqui: se estourar, nada é gravado e a análise é descartada.
   */
  private int save(UUID snapshotId, String kind, String sourceUrl, EvidenceSubject subject, String checkKey,
      List<BaseLinkEvidenceExtractor.Claim> claims) {
    Integer created = transaction.execute(status -> {
      GtSnapshot snapshot = loadAuthorized(snapshotId);
      Owner owner = snapshot.getOwner();
      quota.consumeExtraction(owner);
      Instant now = Instant.now();
      for (BaseLinkEvidenceExtractor.Claim claim : claims) {
        evidence.save(SupplementaryEvidence.builder()
            .owner(owner)
            .snapshot(snapshot)
            .sourceKind(kind)
            .sourceUrl(sourceUrl)
            .subjectUrl(subject == null ? null : subject.link())
            .subjectKey(subject == null ? null : subject.key())
            .subjectLabel(subject == null ? null : subject.label())
            .sourceExcerpt(claim.excerpt())
            .claim(claim.claim())
            .reviewState("pendente")
            .collectedAt(now)
            .build());
      }
      recordCheck(snapshotId, owner.getId(), checkKey, claims.size(), now);
      return claims.size();
    });
    return created == null ? 0 : created;
  }

  /** Marca uma fonte como vista sem análise de IA (busca feita, página irrelevante). */
  private void markChecked(UUID snapshotId, UUID ownerId, String checkKey, int count) {
    transaction.executeWithoutResult(status -> recordCheck(snapshotId, ownerId, checkKey, count, Instant.now()));
  }

  private void recordCheck(UUID snapshotId, UUID ownerId, String checkKey, int count, Instant when) {
    jdbc.update("insert into cv_evidence_link_checks (snapshot_id, source_url, owner_id, checked_at, claims_count) "
        + "values (?, ?, ?, ?, ?) on conflict (snapshot_id, source_url) do nothing",
        snapshotId, checkKey, ownerId, Timestamp.from(when), count);
  }

  public List<SupplementaryEvidenceResponse> findBySnapshot(UUID snapshotId) {
    return transaction.execute(status -> evidence
        .findAllBySnapshotIdOrderByCollectedAtAsc(loadAuthorized(snapshotId).getId())
        .stream().map(SupplementaryEvidenceResponse::from).toList());
  }

  /** Registra a decisão da pessoa. A evidência só é alterada se o owner dela for quem pede. */
  public SupplementaryEvidenceResponse updateReviewState(UUID evidenceId, String reviewState) {
    return transaction.execute(status -> {
      SupplementaryEvidence item = evidence.findById(evidenceId)
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvEvidence.NOT_FOUND));
      access.assertCanAccess(item.getOwner());
      item.setReviewState(reviewState);
      return SupplementaryEvidenceResponse.from(item);
    });
  }

  /** Carrega o snapshot e exige acesso ao owner dele; um snapshot inexistente aparece como não encontrado. */
  private GtSnapshot loadAuthorized(UUID snapshotId) {
    GtSnapshot snapshot = snapshots.findById(snapshotId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND));
    access.assertCanAccess(snapshot.getOwner());
    return snapshot;
  }

  /** URLs distintas presentes nos valores aprovados do snapshot, na ordem estável dos fatos. */
  private List<String> baseLinks(UUID snapshotId) {
    Set<String> urls = new LinkedHashSet<>();
    for (GtSnapshotItem item : snapshotItems.findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(snapshotId)) {
      if (item.getApprovedValue() instanceof String text) {
        Matcher matcher = URL.matcher(text);
        while (matcher.find()) {
          urls.add(stripTrailingPunctuation(matcher.group()));
        }
      }
    }
    return List.copyOf(urls);
  }

  private static String stripTrailingPunctuation(String url) {
    return url.replaceAll("[.,;:)]+$", "");
  }
}
