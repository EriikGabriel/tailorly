import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import { pickToken } from "../../src/stores/active-token";
import {
  ANONYMOUS_TOKEN_PREFIX,
  useAnonymousSessionStore as anonymous,
} from "../../src/stores/anonymous-session-store";
import {
  useAuthStore as auth,
  readStoredSession,
} from "../../src/stores/auth-store";
import { useBaseCvStore as base } from "../../src/stores/base-cv-store";
import { useDocumentViewerStore as viewer } from "../../src/stores/document-viewer-store";
import { useJobDraftStore as job } from "../../src/stores/job-draft-store";
import { useQuickReviewStore as quick } from "../../src/stores/quick-review-store";
import { resetClientState } from "../../src/stores/reset-client-state";
import { useReviewDraftStore as review } from "../../src/stores/review-draft-store";
import { useWorkspaceStore as workspace } from "../../src/stores/workspace-store";
import type { ReviewIssue } from "../../src/types/review";

const pdf = (name = "cv.pdf") =>
  new File(["%PDF-test"], name, { type: "application/pdf" });
const issues: ReviewIssue[] = [
  {
    id: "current",
    category: "employment",
    type: "temporal",
    question: "Vínculo atual?",
    motivation: "Confirmar período",
    choices: [{ value: "yes", label: "Sim", result: "confirmed" }],
  },
];
beforeEach(resetClientState);

test("limpar a sessão remove o token de autenticação", () => {
  auth.getState().setSession({
    accessToken: "test-token",
    expiresAt: "2030-01-01T00:00:00Z",
  });

  resetClientState();

  assert.equal(auth.getState().accessToken, null);
  assert.equal(auth.getState().expiresAt, null);
});

test("a sessão válida pode ser recuperada no reload e a expirada é descartada", () => {
  const values = new Map<string, string>();
  const storage = {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  };
  const previousStorage = Object.getOwnPropertyDescriptor(
    globalThis,
    "sessionStorage",
  );
  Object.defineProperty(globalThis, "sessionStorage", {
    configurable: true,
    value: storage,
  });

  try {
    const expiresAt = new Date(Date.now() + 60_000).toISOString();
    auth.getState().setSession({ accessToken: "test-token", expiresAt });
    assert.deepEqual(readStoredSession(), {
      accessToken: "test-token",
      expiresAt,
    });
    auth.getState().clearSession();
    assert.equal(storage.getItem("tailorly:auth-session"), null);

    storage.setItem(
      "tailorly:auth-session",
      JSON.stringify({
        accessToken: "expired-token",
        expiresAt: new Date(Date.now() - 60_000).toISOString(),
      }),
    );
    assert.equal(readStoredSession(), null);
    assert.equal(storage.getItem("tailorly:auth-session"), null);
  } finally {
    auth.getState().clearSession();
    if (previousStorage) {
      Object.defineProperty(globalThis, "sessionStorage", previousStorage);
    } else {
      delete (globalThis as { sessionStorage?: Storage }).sessionStorage;
    }
  }
});

test("a seleção compartilhada preserva rascunhos, etapa e zoom entre componentes", () => {
  const file = pdf();
  base.getState().selectFile(file);
  const sourceId = base.getState().selectionId;
  review.getState().setIdentity(sourceId, { name: "Pessoa de teste" });
  review.getState().setActiveStep("skills");
  viewer.getState().setZoom(125);
  base.getState().selectFile(file);
  assert.equal(base.getState().file, file);
  assert.equal(review.getState().draft.identity.name, "Pessoa de teste");
  assert.equal(review.getState().activeStep, "skills");
  assert.equal(viewer.getState().zoom, 125);
});

test("o ID do upload acompanha a seleção e ignora respostas atrasadas", () => {
  const file = pdf();
  base.getState().selectFile(file);
  const firstSelection = base.getState().selectionId;
  base.getState().setUploadedDocument(file);
  assert.equal(base.getState().uploadedSelectionId, firstSelection);

  base.getState().selectFile(pdf("next.pdf"));
  base.getState().setUploadedDocument(file);
  assert.equal(base.getState().uploadedSelectionId, null);

  resetClientState();
  assert.equal(base.getState().uploadedSelectionId, null);
});

test("trocar o currículo base limpa dados dependentes e rejeita atualizações antigas", () => {
  base.getState().selectFile(pdf());
  const oldId = base.getState().selectionId;
  review.getState().setIdentity(oldId, { name: "Original" });
  review
    .getState()
    .setCollection(oldId, "experience", [
      { id: "role", fields: { company: "Teste" } },
    ]);
  review.getState().setExpandedRecord(oldId, "experience", "role");
  review.getState().setActiveStep("experience");
  quick.getState().configure("old", issues, true);
  quick
    .getState()
    .setAnswer("old", "current", { choice: "yes", correction: "" });
  viewer.getState().setZoom(150);
  job.getState().setJobText("Vaga");
  base.getState().selectFile(pdf("next.pdf"));
  review.getState().setIdentity(oldId, { name: "Resposta atrasada" });
  assert.notEqual(base.getState().selectionId, oldId);
  assert.deepEqual(review.getState().draft.identity, {});
  assert.deepEqual(review.getState().draft.experience, []);
  assert.deepEqual(review.getState().expandedRecords, {});
  assert.equal(review.getState().activeStep, "identity");
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(viewer.getState().zoom, "fit");
  assert.equal(job.getState().jobText, "Vaga");
});

test("arquivos inválidos ou vazios preservam o arquivo e o rascunho atuais", () => {
  const file = pdf();
  base.getState().selectFile(file);
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Preservado" });
  assert.equal(base.getState().selectFile(new File([], "empty.pdf")), false);
  assert.equal(base.getState().selectFile(pdf("not-a-pdf.docx")), false);
  assert.equal(
    base
      .getState()
      .selectFile(
        new File([new Uint8Array(7 * 1024 * 1024 + 1)], "oversized.pdf"),
      ),
    false,
  );
  assert.equal(base.getState().file, file);
  assert.equal(review.getState().draft.identity.name, "Preservado");
  assert.ok(base.getState().error);
});

test("remover o arquivo limpa rascunhos, revisão rápida e visualizador", () => {
  base.getState().selectFile(pdf());
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Apagar" });
  quick.getState().configure("source", issues, true);
  quick
    .getState()
    .setAnswer("source", "current", { choice: "yes", correction: "" });
  quick.getState().setOpen(true);
  viewer.getState().setZoom(200);
  base.getState().selectFile(null);
  assert.equal(base.getState().selectionId, null);
  assert.equal(base.getState().file, null);
  assert.deepEqual(review.getState().draft.identity, {});
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(quick.getState().open, false);
  assert.equal(viewer.getState().zoom, "fit");
});

test("alterar a vaga invalida a revisão rápida sem apagar o rascunho do currículo base", () => {
  base.getState().selectFile(pdf());
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Preservado" });
  quick.getState().configure("job-1", issues, true);
  quick
    .getState()
    .setAnswer("job-1", "current", { choice: "yes", correction: "" });
  const previousRevision = job.getState().revision;
  job.getState().setJobUrl("https://example.com/job");
  assert.equal(job.getState().revision, previousRevision + 1);
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(review.getState().draft.identity.name, "Preservado");
});

test("a revisão rápida preserva respostas ao remontar e rejeita pendências antigas ou desconhecidas", () => {
  quick.getState().configure("a", issues, true);
  quick.getState().setAnswer("a", "current", { choice: "yes", correction: "" });
  quick.getState().savePreview("a");
  quick.getState().configure("a", issues, true);
  assert.equal(quick.getState().answers.current.choice, "yes");
  assert.ok(quick.getState().submittedPreview);
  quick.getState().configure("b", issues, true);
  quick.getState().setAnswer("a", "current", { choice: "yes", correction: "" });
  quick.getState().setAnswer("b", "missing", { choice: "yes", correction: "" });
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(quick.getState().submittedPreview, null);
});

test("limpar a sessão apaga os contextos e restaura os padrões", () => {
  base.getState().selectFile(pdf());
  job.getState().setJobText("Dados de uma sessão anterior");
  job.getState().saveDraft();
  workspace.getState().setActiveView("preview");
  resetClientState();
  assert.equal(base.getState().file, null);
  assert.equal(job.getState().jobText, "");
  assert.equal(job.getState().savedAt, null);
  assert.equal(workspace.getState().activeView, "editor");
});

test("o visualizador limita o zoom e ignora valores numéricos inválidos", () => {
  viewer.getState().setZoom(500);
  assert.equal(viewer.getState().zoom, 200);
  viewer.getState().setZoom(1);
  assert.equal(viewer.getState().zoom, 50);
  viewer.getState().setZoom(Number.NaN);
  assert.equal(viewer.getState().zoom, 50);
  viewer.getState().setZoom("fit");
  assert.equal(viewer.getState().zoom, "fit");
});

const visitorSession = (overrides: Partial<{ expiresAt: string }> = {}) => ({
  token: `${ANONYMOUS_TOKEN_PREFIX}abc`,
  sessionId: "session-1",
  ownerId: "owner-1",
  expiresAt: new Date(Date.now() + 60_000).toISOString(),
  ...overrides,
});

test("a sessão anônima fica no sessionStorage e a expirada é descartada na leitura", () => {
  const values = new Map<string, string>();
  const storage = {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  };
  const previous = Object.getOwnPropertyDescriptor(
    globalThis,
    "sessionStorage",
  );
  Object.defineProperty(globalThis, "sessionStorage", {
    configurable: true,
    value: storage,
  });

  try {
    anonymous.getState().setSession(visitorSession());
    assert.ok(
      storage.getItem("tailorly:anonymous-session")?.includes("session-1"),
    );

    anonymous.getState().clear();
    assert.equal(storage.getItem("tailorly:anonymous-session"), null);
    assert.equal(anonymous.getState().session, null);
  } finally {
    anonymous.getState().clear();
    if (previous) Object.defineProperty(globalThis, "sessionStorage", previous);
    else delete (globalThis as { sessionStorage?: Storage }).sessionStorage;
  }
});

test("limpar o estado do cliente também descarta a sessão anônima e o tipo de owner", () => {
  anonymous.getState().setSession(visitorSession());
  base.getState().selectFile(pdf());
  base
    .getState()
    .setUploadedDocument(base.getState().file as File, "v1", "anonymous");
  assert.equal(base.getState().ownerKind, "anonymous");

  resetClientState();

  assert.equal(anonymous.getState().session, null);
  assert.equal(base.getState().ownerKind, null);
});

test("trocar o arquivo zera o tipo de owner; o envio o define", () => {
  const first = pdf();
  base.getState().selectFile(first);
  base.getState().setUploadedDocument(first, "v1", "account");
  assert.equal(base.getState().ownerKind, "account");

  base.getState().selectFile(pdf("next.pdf"));
  assert.equal(base.getState().ownerKind, null);
  assert.equal(base.getState().baseVersionId, null);
});

test("o token ativo respeita de quem é a versão enviada", () => {
  const accountToken = "jwt-da-conta";
  const anonymousToken = `${ANONYMOUS_TOKEN_PREFIX}abc`;

  // Visitante sem conta usa a sessão anônima.
  assert.equal(
    pickToken({ accountToken: null, anonymousToken, ownerKind: null }),
    anonymousToken,
  );
  // Conta sem sessão anônima usa o JWT.
  assert.equal(
    pickToken({ accountToken, anonymousToken: null, ownerKind: null }),
    accountToken,
  );
  // Entrou numa conta com um novo envio pendente: a conta vale.
  assert.equal(
    pickToken({ accountToken, anonymousToken, ownerKind: "account" }),
    accountToken,
  );
  assert.equal(
    pickToken({ accountToken, anonymousToken, ownerKind: null }),
    accountToken,
  );
  // Versão enviada como visitante segue acessível pelo token anônimo mesmo após o login.
  assert.equal(
    pickToken({ accountToken, anonymousToken, ownerKind: "anonymous" }),
    anonymousToken,
  );
  // Sem sessão anônima, cai para a conta; sem nada, não há token.
  assert.equal(
    pickToken({ accountToken, anonymousToken: null, ownerKind: "anonymous" }),
    accountToken,
  );
  assert.equal(
    pickToken({ accountToken: null, anonymousToken: null, ownerKind: null }),
    null,
  );
});
