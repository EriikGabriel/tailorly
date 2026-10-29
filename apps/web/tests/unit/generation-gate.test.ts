import assert from "node:assert/strict";
import test from "node:test";
import {
  extractionState,
  generationBlockReason,
} from "../../src/utils/generation-gate";
import { checkJobUrl } from "../../src/utils/job-url";

const base = { hasFile: true, versionId: "v1", queryFailed: false };

test("só a execução concluída libera a geração", () => {
  assert.equal(extractionState({ ...base, runStatus: "COMPLETED" }), "ready");
  assert.equal(extractionState({ ...base, runStatus: "QUEUED" }), "processing");
  assert.equal(
    extractionState({ ...base, runStatus: "PROCESSING" }),
    "processing",
  );
  assert.equal(extractionState({ ...base, runStatus: "FAILED" }), "failed");
  assert.equal(extractionState({ ...base, runStatus: "ALGO_NOVO" }), "failed");
  assert.equal(extractionState({ ...base, runStatus: undefined }), "waiting");
});

test("sem arquivo, enviando ou com erro de consulta, bloqueia", () => {
  assert.equal(
    extractionState({ ...base, hasFile: false, runStatus: "COMPLETED" }),
    "missing",
  );
  assert.equal(
    extractionState({ ...base, versionId: null, runStatus: undefined }),
    "uploading",
  );
  assert.equal(
    extractionState({ ...base, queryFailed: true, runStatus: "COMPLETED" }),
    "unknown",
  );
});

const ok = checkJobUrl("https://acme.gupy.io/jobs/1");

test("extração concluída e link válido liberam; o motivo é nulo", () => {
  assert.equal(
    generationBlockReason({
      extraction: "ready",
      mode: "url",
      urlCheck: ok,
      hasJobText: false,
    }),
    null,
  );
  assert.equal(
    generationBlockReason({
      extraction: "ready",
      mode: "text",
      urlCheck: checkJobUrl(""),
      hasJobText: true,
    }),
    null,
  );
});

test("a extração tem prioridade sobre a vaga nos motivos", () => {
  for (const state of [
    "missing",
    "uploading",
    "unknown",
    "waiting",
    "processing",
    "failed",
  ] as const) {
    const reason = generationBlockReason({
      extraction: state,
      mode: "url",
      urlCheck: ok,
      hasJobText: true,
    });
    assert.ok(reason, state);
  }
  assert.match(
    generationBlockReason({
      extraction: "processing",
      mode: "url",
      urlCheck: ok,
      hasJobText: true,
    }) ?? "",
    /extração/,
  );
});

test("link vazio, inválido ou com aviso bloqueiam; link válido não", () => {
  const reason = (value: string) =>
    generationBlockReason({
      extraction: "ready",
      mode: "url",
      urlCheck: checkJobUrl(value),
      hasJobText: true,
    });

  assert.match(reason("") ?? "", /Informe o link/);
  assert.match(reason("acme.gupy.io/x") ?? "", /https/);
  assert.match(
    reason("https://www.linkedin.com/jobs/view/1") ?? "",
    /Cole o texto/,
  );
  assert.equal(reason("https://acme.gupy.io/x"), null);
});

test("no modo texto o link é ignorado, mas o texto é obrigatório", () => {
  assert.match(
    generationBlockReason({
      extraction: "ready",
      mode: "text",
      urlCheck: checkJobUrl("lixo"),
      hasJobText: false,
    }) ?? "",
    /texto da vaga/,
  );
  assert.equal(
    generationBlockReason({
      extraction: "ready",
      mode: "text",
      urlCheck: checkJobUrl("lixo"),
      hasJobText: true,
    }),
    null,
  );
});
