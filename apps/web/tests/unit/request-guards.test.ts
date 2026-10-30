import assert from "node:assert/strict";
import test from "node:test";
import { ApiClientError } from "../../src/lib/api/client";
import { shouldRetryRequest } from "../../src/lib/api/retry-policy";
import {
  type AnonymousSession,
  isAnonymousSessionActive,
} from "../../src/stores/anonymous-session-store";
import {
  EXTRACTION_POLL_MS,
  extractionJustCompleted,
  extractionRunInterval,
  interpretationRefetchInterval,
} from "../../src/utils/extraction-polling";
import {
  jobPostingInputError,
  MAX_JOB_TEXT_LENGTH,
} from "../../src/utils/job-posting-input";

test("não repete resposta 4xx, só falha de rede e 5xx, uma única vez", () => {
  for (const status of [400, 401, 403, 404, 409, 422, 429]) {
    assert.equal(shouldRetryRequest(0, new ApiClientError(status, {})), false);
  }
  assert.equal(shouldRetryRequest(0, new ApiClientError(503, {})), true);
  assert.equal(shouldRetryRequest(0, new TypeError("Failed to fetch")), true);
  assert.equal(shouldRetryRequest(1, new TypeError("Failed to fetch")), false);
  assert.equal(shouldRetryRequest(1, new ApiClientError(500, {})), false);
});

test("erro de pré-condição do cliente não é repetido", () => {
  assert.equal(shouldRetryRequest(0, new Error("Sessão expirada")), false);
});

test("vaga: bloqueia entrada vazia ou inválida antes de qualquer chamada", () => {
  assert.equal(
    jobPostingInputError({ mode: "url", url: "  " }),
    "Informe o link da vaga.",
  );
  assert.match(
    jobPostingInputError({ mode: "url", url: "acme.gupy.io/jobs/1" }) ?? "",
    /https/,
  );
  assert.equal(
    jobPostingInputError({ mode: "url", url: "https://acme.gupy.io/jobs/1" }),
    null,
  );
  assert.equal(
    jobPostingInputError({ mode: "text", text: " \n " }),
    "Cole o texto da vaga.",
  );
  assert.equal(
    jobPostingInputError({ mode: "text", text: "Vaga de dev" }),
    null,
  );
  assert.match(
    jobPostingInputError({
      mode: "text",
      text: "a".repeat(MAX_JOB_TEXT_LENGTH + 1),
    }) ?? "",
    /passa de/,
  );
});

test("polling da execução só enquanto está na fila ou processando", () => {
  assert.equal(extractionRunInterval(undefined), false);
  assert.equal(extractionRunInterval({ status: "QUEUED" }), EXTRACTION_POLL_MS);
  assert.equal(
    extractionRunInterval({ status: "PROCESSING" }),
    EXTRACTION_POLL_MS,
  );
  assert.equal(extractionRunInterval({ status: "COMPLETED" }), false);
  assert.equal(extractionRunInterval({ status: "FAILED" }), false);
});

test("fatos não são consultados em ciclo enquanto a execução roda", () => {
  const running = { status: "PROCESSING", finishedAt: null };
  assert.equal(
    interpretationRefetchInterval({ latestRun: running, loadedCount: 0 }),
    false,
  );
});

test("resultado vazio logo após concluir é reconsultado por pouco tempo", () => {
  const now = Date.parse("2026-10-30T12:00:30Z");
  const recent = { status: "COMPLETED", finishedAt: "2026-10-30T12:00:00Z" };
  const old = { status: "COMPLETED", finishedAt: "2026-10-30T11:00:00Z" };
  assert.equal(
    interpretationRefetchInterval({ latestRun: recent, loadedCount: 0, now }),
    EXTRACTION_POLL_MS,
  );
  assert.equal(
    interpretationRefetchInterval({ latestRun: old, loadedCount: 0, now }),
    false,
  );
  assert.equal(
    interpretationRefetchInterval({ latestRun: recent, loadedCount: 3, now }),
    false,
  );
});

test("a consulta única de fatos dispara só na transição para concluída", () => {
  assert.equal(extractionJustCompleted("PROCESSING", "COMPLETED"), true);
  assert.equal(extractionJustCompleted("QUEUED", "COMPLETED"), true);
  assert.equal(extractionJustCompleted(undefined, "COMPLETED"), false);
  assert.equal(extractionJustCompleted("COMPLETED", "COMPLETED"), false);
  assert.equal(extractionJustCompleted("PROCESSING", "FAILED"), false);
});

test("sessão anônima vencida não é usada nas chamadas", () => {
  const session: AnonymousSession = {
    token: "tlr_anon_x",
    sessionId: "s",
    ownerId: "o",
    expiresAt: "2026-10-30T12:00:00Z",
  };
  const before = Date.parse("2026-10-30T11:59:59Z");
  const after = Date.parse("2026-10-30T12:00:00Z");
  assert.equal(isAnonymousSessionActive(session, before), true);
  assert.equal(isAnonymousSessionActive(session, after), false);
  assert.equal(isAnonymousSessionActive(null, before), false);
});
