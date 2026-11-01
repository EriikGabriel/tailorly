package com.tailorly.api.cv.progress;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * Andamento da geração em curso de cada owner, para a interface mostrar a etapa atual (reescrita
 * pela IA, compilação, nota de ATS...) em vez de só esperar a resposta. Fica em memória e é
 * descartável: não guarda dados do currículo, só rótulos de etapa e contagens, e some pouco depois
 * de terminar. Numa instalação com várias instâncias da API, a consulta precisa cair na instância
 * que gera; sem isso a interface cai para um indicador genérico.
 */
@Component
public class GenerationProgress {
  public static final String PREPARING = "PREPARING";
  public static final String REWRITING = "REWRITING";
  public static final String RENDERING = "RENDERING";
  public static final String SCORING = "SCORING";
  public static final String REFINING = "REFINING";
  public static final String SAVING = "SAVING";

  public static final String RUNNING = "RUNNING";
  public static final String DONE = "DONE";
  public static final String FAILED = "FAILED";

  private static final Duration KEEP_FINISHED = Duration.ofSeconds(30);
  private static final Duration KEEP_STALE = Duration.ofMinutes(15);

  /** {@code round} conta as rodadas de ajuste de layout pela IA (0 enquanto não houve nenhuma). */
  public record Snapshot(String status, String stage, String detail, int round, Instant startedAt,
      Instant updatedAt) {
  }

  /** O que a esteira usa para informar etapas; um valor no-op serve para chamadas sem acompanhamento. */
  public interface Tracker {
    Tracker NONE = (stage, detail, round) -> {
    };

    void step(String stage, String detail, int round);

    default void step(String stage, String detail) {
      step(stage, detail, 0);
    }
  }

  private final ConcurrentMap<UUID, Snapshot> byOwner = new ConcurrentHashMap<>();

  public Tracker begin(UUID ownerId) {
    prune();
    Instant now = Instant.now();
    byOwner.put(ownerId, new Snapshot(RUNNING, PREPARING, null, 0, now, now));
    return (stage, detail, round) -> byOwner.computeIfPresent(ownerId, (id, current) ->
        new Snapshot(RUNNING, stage, detail, round, current.startedAt(), Instant.now()));
  }

  public void finish(UUID ownerId) {
    end(ownerId, DONE);
  }

  public void fail(UUID ownerId) {
    end(ownerId, FAILED);
  }

  public Optional<Snapshot> current(UUID ownerId) {
    prune();
    return Optional.ofNullable(byOwner.get(ownerId));
  }

  private void end(UUID ownerId, String status) {
    byOwner.computeIfPresent(ownerId, (id, current) ->
        new Snapshot(status, current.stage(), current.detail(), current.round(), current.startedAt(), Instant.now()));
  }

  private void prune() {
    Instant now = Instant.now();
    byOwner.entrySet().removeIf(entry -> {
      Snapshot snapshot = entry.getValue();
      Duration limit = RUNNING.equals(snapshot.status()) ? KEEP_STALE : KEEP_FINISHED;
      return Duration.between(snapshot.updatedAt(), now).compareTo(limit) > 0;
    });
  }
}
