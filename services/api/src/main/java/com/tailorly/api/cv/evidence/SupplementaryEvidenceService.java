package com.tailorly.api.cv.evidence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.base.snapshot.GtSnapshotItem;
import com.tailorly.api.base.snapshot.GtSnapshotItemRepository;
import com.tailorly.api.base.snapshot.GtSnapshotRepository;
import com.tailorly.api.cv.evidence.dto.SupplementaryEvidenceResponse;
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
 * Coleta evidências complementares de três fontes, sempre sobre projetos do snapshot:
 * <ul>
 *   <li><b>base_link</b>: os links já presentes nos fatos aprovados;</li>
 *   <li><b>web</b>: páginas encontradas por busca pelo título do projeto (se houver provedor configurado);</li>
 *   <li><b>vault</b>: notas do Obsidian que citam o título do projeto (se a leitura estiver habilitada).</li>
 * </ul>
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
  static final int MAX_ANALYSES = 5;
  static final int MAX_SEARCH_PROJECTS = 3;
  static final int SEARCH_RESULTS = 3;
  static final int MAX_WEB_PAGES_PER_PROJECT = 2;
  private static final Logger log = LoggerFactory.getLogger(SupplementaryEvidenceService.class);
  private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+");
  private static final Pattern PROJECT_FIELD = Pattern.compile("^(\\d{1,3}):(title|link)$");

  private final GtSnapshotRepository snapshots;
  private final GtSnapshotItemRepository snapshotItems;
  private final OwnerAccessService access;
  private final SupplementaryEvidenceRepository evidence;
  private final JobPageFetcher pageFetcher;
  private final BaseLinkEvidenceExtractor extractor;
  private final AiPreferencesService preferences;
  private final AnonymousQuotaService quota;
  private final WebSearchClient webSearch;
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

  private record Project(String title, String link) {
  }

  private record Plan(UUID ownerId, int linksFound, int alreadyKnown, List<String> pendingLinks,
      List<Project> projects, Set<String> analyzed, boolean aiAvailable, Optional<Path> vaultDir, boolean webEnabled) {
  }

  /** Contador de uma chamada: análises de IA restantes e falhas acumuladas. */
  private static final class Run {
    int budget = MAX_ANALYSES;
    int created;
    int failed;
    String aiStatus = "completed";
  }

  public CollectionResult collect(UUID snapshotId) {
    Plan plan = transaction.execute(status -> plan(snapshotId));
    if (!plan.aiAvailable()) {
      return new CollectionResult(plan.linksFound(), plan.alreadyKnown(), 0, 0, "unavailable");
    }

    Run run = new Run();
    try {
      for (String link : plan.pendingLinks()) {
        if (run.budget == 0) {
          break;
        }
        attempt(run, () -> analyzePage(snapshotId, plan.ownerId(), "base_link", link, link, link));
      }
      collectProjectSources(snapshotId, plan, run);
    } catch (TooManyRequestsException limit) {
      run.aiStatus = "limit_reached";
    }
    return new CollectionResult(plan.linksFound(), plan.alreadyKnown(), run.failed, run.created, run.aiStatus);
  }

  /** Busca e leitura de vault para cada projeto, dentro do orçamento da chamada. */
  private void collectProjectSources(UUID snapshotId, Plan plan, Run run) {
    boolean searchEnabled = plan.webEnabled();
    int searched = 0;
    for (Project project : plan.projects()) {
      if (searchEnabled && searched < MAX_SEARCH_PROJECTS && run.budget > 0) {
        String searchKey = "search:" + project.link();
        if (!plan.analyzed().contains(searchKey)) {
          searched++;
          List<WebSearchClient.SearchResult> results = searchOrEmpty(project.title());
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
            attempt(run, () -> analyzeWebResult(snapshotId, plan.ownerId(), project, result.url()));
          }
        }
      }
      if (plan.vaultDir().isPresent()) {
        for (VaultNoteReader.Note note : vault.findMentioning(plan.vaultDir().get(), project.title())) {
          String key = "vault:" + note.key();
          if (plan.analyzed().contains(key) || run.budget == 0) {
            continue;
          }
          attempt(run, () -> analyzeText(snapshotId, plan.ownerId(), "vault", null, project.link(), key, note.text()));
        }
      }
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

  /** Página de link do Currículo Base: sempre analisada se baixada (o link é do próprio projeto). */
  private int analyzePage(UUID snapshotId, UUID ownerId, String kind, String url, String subject, String key) {
    FetchedPage page = pageFetcher.fetch(url);
    return analyzeText(snapshotId, ownerId, kind, url, subject, key, pageText(page));
  }

  /** Página encontrada pela busca: só conta se citar o título do projeto; senão é marcada e descartada. */
  private int analyzeWebResult(UUID snapshotId, UUID ownerId, Project project, String url) {
    FetchedPage page = pageFetcher.fetch(url);
    String text = pageText(page);
    if (!Text.mentions(text, project.title())) {
      markChecked(snapshotId, ownerId, url, 0);
      return 0;
    }
    return analyzeText(snapshotId, ownerId, "web", url, project.link(), url, text);
  }

  private int analyzeText(UUID snapshotId, UUID ownerId, String kind, String sourceUrl, String subjectUrl,
      String checkKey, String text) {
    List<BaseLinkEvidenceExtractor.Claim> claims = extractor.extract(ownerId, text);
    return save(snapshotId, kind, sourceUrl, subjectUrl, checkKey, claims);
  }

  private static String pageText(FetchedPage page) {
    return page.document().body() == null ? page.document().text() : page.document().body().text();
  }

  private Plan plan(UUID snapshotId) {
    GtSnapshot snapshot = loadAuthorized(snapshotId);
    UUID ownerId = snapshot.getOwner().getId();

    List<String> links = baseLinks(snapshot.getId());
    Set<String> analyzed = new HashSet<>(jdbc.queryForList(
        "select source_url from cv_evidence_link_checks where snapshot_id = ?", String.class, snapshot.getId()));
    List<String> pending = links.stream().filter(link -> !analyzed.contains(link)).toList();
    int alreadyKnown = (int) links.stream().filter(analyzed::contains).count();

    boolean aiAvailable = !"rules".equals(preferences.resolveEffectiveSettings(ownerId).provider());
    return new Plan(ownerId, links.size(), alreadyKnown, aiAvailable ? pending : List.of(),
        projects(snapshot.getId()), analyzed, aiAvailable, ownerVaults.readableVaultOf(ownerId),
        webSearch.isConfigured() && ownerVaults.webSearchEnabledFor(ownerId));
  }

  /**
   * Grava os detalhes de uma fonte e a marca como analisada, na mesma transação. O limite de
   * processamento é consumido aqui: se estourar, nada é gravado e a análise é descartada.
   */
  private int save(UUID snapshotId, String kind, String sourceUrl, String subjectUrl, String checkKey,
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
            .subjectUrl(subjectUrl)
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

  /** Projetos do snapshot que têm título e link; sem link não há a que o enriquecimento casar. */
  private List<Project> projects(UUID snapshotId) {
    TreeMap<Integer, String[]> byIndex = new TreeMap<>();
    for (GtSnapshotItem item : snapshotItems.findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(snapshotId)) {
      if (!"projects".equals(item.getCategory()) || !(item.getApprovedValue() instanceof String value)) {
        continue;
      }
      Matcher matcher = PROJECT_FIELD.matcher(item.getFieldKey());
      if (matcher.matches()) {
        String[] pair = byIndex.computeIfAbsent(Integer.parseInt(matcher.group(1)), index -> new String[2]);
        pair["title".equals(matcher.group(2)) ? 0 : 1] = value.strip();
      }
    }
    List<Project> projects = new ArrayList<>();
    for (String[] pair : byIndex.values()) {
      if (pair[0] != null && pair[1] != null && pair[1].startsWith("http")) {
        projects.add(new Project(pair[0], stripTrailingPunctuation(pair[1])));
      }
    }
    return List.copyOf(projects);
  }

  private static String stripTrailingPunctuation(String url) {
    return url.replaceAll("[.,;:)]+$", "");
  }
}
