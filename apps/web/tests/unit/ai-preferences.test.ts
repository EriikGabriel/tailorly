import assert from "node:assert/strict";
import test from "node:test";
import type {
  AiPreferencesResponse,
  LocalCliPreferencesResponse,
} from "@api/generated/model";
import {
  aiDraftFromResponse,
  aiPreferencesRequestFromDraft,
  isAiDraftValid,
  isLocalCliDraftValid,
} from "@utils/ai-preferences";

const efforts = {
  default: "medium",
  interpretation: "high",
  "job-analysis": "medium",
  "resume-layout-plan": "low",
};

function cli(
  patch: Partial<LocalCliPreferencesResponse> = {},
): LocalCliPreferencesResponse {
  return {
    available: true,
    model: "sonnet",
    timeoutSeconds: 180,
    efforts,
    serverModel: "sonnet",
    serverTimeoutSeconds: 180,
    serverEfforts: efforts,
    ...patch,
  };
}

function response(
  patch: Partial<AiPreferencesResponse> = {},
): AiPreferencesResponse {
  return {
    provider: "claude-cli",
    accountOverride: true,
    remoteEndpoint: null,
    remoteModel: null,
    remoteApiKeyConfigured: false,
    ollamaEndpoint: "http://127.0.0.1:11434/v1/chat/completions",
    ollamaModel: "qwen3:8b",
    ollamaApiKeyConfigured: false,
    claudeCli: cli(),
    codexCli: cli({ model: "", serverModel: "" }),
    localCliTasks: ["interpretation", "job-analysis", "resume-layout-plan"],
    updatedAt: null,
    ...patch,
  };
}

test("claude-cli habilitado e válido envia modelo, timeout e esforços", () => {
  const draft = aiDraftFromResponse(response());
  draft.localCli["claude-cli"].efforts["job-analysis"] = "xhigh";

  assert.equal(isAiDraftValid(draft), true);
  const request = aiPreferencesRequestFromDraft(draft);
  assert.equal(request.provider, "claude-cli");
  assert.equal(request.claudeCli?.model, "sonnet");
  assert.equal(request.claudeCli?.timeoutSeconds, 180);
  assert.equal(request.claudeCli?.efforts?.["job-analysis"], "xhigh");
  // O rascunho não altera os padrões do servidor.
  assert.equal(
    draft.localCliServer["claude-cli"].defaults.efforts["job-analysis"],
    "medium",
  );
});

test("codex-cli aceita modelo vazio (padrão do Codex) e envia o próprio bloco", () => {
  const draft = aiDraftFromResponse(response({ provider: "codex-cli" }));
  draft.localCli["codex-cli"].efforts.interpretation = "max";

  assert.equal(isAiDraftValid(draft), true);
  const request = aiPreferencesRequestFromDraft(draft);
  assert.equal(request.codexCli?.model, "");
  assert.equal(request.codexCli?.efforts?.interpretation, "max");
  assert.equal(request.claudeCli?.model, "sonnet");
});

test("CLI local não é válido quando o servidor não o habilita", () => {
  const draft = aiDraftFromResponse(
    response({ codexCli: cli({ available: false }), provider: "codex-cli" }),
  );

  assert.equal(isAiDraftValid(draft), false);
  assert.equal(aiPreferencesRequestFromDraft(draft).codexCli, undefined);
});

test("modelo vira argumento do CLI: sem espaço nem hífen inicial", () => {
  const base = { timeoutSeconds: 180, efforts: {} };

  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, model: "opus" }),
    true,
  );
  assert.equal(
    isLocalCliDraftValid("claude-cli", {
      ...base,
      model: "claude-sonnet-5-5[1m]",
    }),
    true,
  );
  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, model: "--verbose" }),
    false,
  );
  assert.equal(
    isLocalCliDraftValid("codex-cli", { ...base, model: "a b" }),
    false,
  );
  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, model: "" }),
    false,
  );
  assert.equal(isLocalCliDraftValid("codex-cli", { ...base, model: "" }), true);
});

test("timeout fora de 10..1800 s é inválido", () => {
  const base = { model: "sonnet", efforts: {} };

  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, timeoutSeconds: 9 }),
    false,
  );
  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, timeoutSeconds: 1801 }),
    false,
  );
  assert.equal(
    isLocalCliDraftValid("claude-cli", { ...base, timeoutSeconds: Number.NaN }),
    false,
  );
  assert.equal(
    isLocalCliDraftValid("codex-cli", { ...base, timeoutSeconds: 600 }),
    true,
  );
});

test("resposta de um backend antigo sem os CLIs locais não quebra", () => {
  const legacy = response({ provider: "rules" });
  for (const key of ["claudeCli", "codexCli", "localCliTasks"] as const)
    delete (legacy as Partial<AiPreferencesResponse>)[key];

  const draft = aiDraftFromResponse(legacy);
  assert.equal(draft.localCliServer["claude-cli"].available, false);
  assert.equal(draft.localCliServer["codex-cli"].available, false);
  assert.deepEqual(draft.localCliTasks, []);
  assert.equal(isAiDraftValid(draft), true);
  assert.equal(aiPreferencesRequestFromDraft(draft).claudeCli, undefined);
});
