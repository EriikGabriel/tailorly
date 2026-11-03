import { expect, type Page, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const snapshotId = "66666666-6666-6666-6666-666666666666";
const versionId = "55555555-5555-5555-5555-555555555555";
const jobPostingId = "77777777-7777-7777-7777-777777777777";

type Evidence = {
  id: string;
  sourceKind: string;
  sourceUrl: string | null;
  subjectKey: string;
  subjectLabel: string;
  sourceExcerpt: string;
  claim: string;
  reviewState: "pendente" | "confirmado" | "descartado";
  collectedAt: string;
};

const evidenceFixture = (): Evidence[] => [
  {
    id: "e1",
    sourceKind: "vault",
    sourceUrl: null,
    subjectKey: "projects:0",
    subjectLabel: "UFSCar Planner",
    sourceExcerpt: "300 alunos usam o app",
    claim: "Usado por 300 alunos",
    reviewState: "pendente",
    collectedAt: "2026-11-01T10:00:00Z",
  },
  {
    id: "e2",
    sourceKind: "web",
    sourceUrl: "https://example.com/a",
    subjectKey: "projects:0",
    subjectLabel: "UFSCar Planner",
    sourceExcerpt: "feito em Kotlin",
    claim: "Feito em Kotlin",
    reviewState: "confirmado",
    collectedAt: "2026-11-01T10:00:00Z",
  },
  {
    id: "e3",
    sourceKind: "vault",
    sourceUrl: null,
    subjectKey: "experience:0",
    subjectLabel: "Tech Lead — FinTech",
    sourceExcerpt: "reduziu custos em 20%",
    claim: "Reduziu custos em 20%",
    reviewState: "pendente",
    collectedAt: "2026-11-01T10:00:00Z",
  },
];

/** Conta com Ground Truth ativo, sem pendências de fatos e com as evidências informadas. */
async function mockAccount(page: Page, evidence: Evidence[]) {
  const calls = { collect: 0, generate: 0 };
  await page.addInitScript(() => {
    sessionStorage.setItem(
      "tailorly:auth-session",
      JSON.stringify({
        accessToken: "t",
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
        refreshToken: "r",
      }),
    );
  });
  await page.route("**/v1/**", (route) =>
    route.fulfill({
      status: 404,
      json: { status: 404, message: "não simulado" },
    }),
  );
  await page.route("**/v1/me", (route) =>
    route.fulfill({
      json: {
        owner: {
          id: ownerId,
          kind: "account",
          userId: "u",
          activeSnapshotId: snapshotId,
          expiresAt: null,
          revision: 0,
        },
        user: {
          id: "u",
          email: "t@example.com",
          enabled: true,
          roles: ["ROLE_USER"],
        },
      },
    }),
  );
  await page.route("**/v1/base-documents/upload?*", (route) =>
    route.fulfill({
      status: 201,
      json: {
        document: {
          id: "33333333-3333-3333-3333-333333333333",
          ownerId,
          originalObjectId: "44444444-4444-4444-4444-444444444444",
          contentHash: "hash",
          originalFilename: "curriculo-exemplo.pdf",
          pageCount: 1,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: versionId,
          ownerId,
          documentId: "33333333-3333-3333-3333-333333333333",
          versionNumber: 1,
          status: "EXTRACTED",
          reviewRevision: 0,
          temporalReviewRequired: false,
          createdAt: new Date().toISOString(),
        },
        versionReused: false,
      },
    }),
  );
  await page.route("**/v1/extraction-runs/version/*", (route) =>
    route.fulfill({
      json: [
        {
          id: "run-1",
          baseVersionId: versionId,
          status: "COMPLETED",
          createdAt: new Date().toISOString(),
          finishedAt: new Date(Date.now() - 120_000).toISOString(),
        },
      ],
    }),
  );
  await page.route("**/v1/fact-records/version/*", (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route("**/v1/review-issues/version/*", (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route("**/v1/job-postings", (route) =>
    route.fulfill({
      status: 201,
      json: {
        id: jobPostingId,
        jobPostingId,
        versionNumber: 1,
        contentHash: "job-hash",
        analysisStatus: "unavailable",
        details: { title: "Vaga de teste", descriptionText: "Vaga fictícia." },
        createdAt: new Date().toISOString(),
        lastCheckedAt: new Date().toISOString(),
      },
    }),
  );
  await page.route(`**/v1/job-postings/${jobPostingId}`, (route) =>
    route.fulfill({
      json: {
        posting: {
          id: jobPostingId,
          ownerId,
          sourceKind: "text",
          latestVersionNumber: 1,
        },
        latest: {
          id: jobPostingId,
          jobPostingId,
          versionNumber: 1,
          contentHash: "job-hash",
          analysisStatus: "unavailable",
          details: {
            title: "Vaga de teste",
            descriptionText: "Vaga fictícia.",
          },
          createdAt: new Date().toISOString(),
          lastCheckedAt: new Date().toISOString(),
        },
        versions: [{ versionNumber: 1, createdAt: new Date().toISOString() }],
      },
    }),
  );
  await page.route("**/v1/cv/evidence/snapshots/*", (route) =>
    route.fulfill({ json: evidence }),
  );
  // The collection request may carry `jobPostingId` in the query string.
  await page.route("**/v1/cv/evidence/snapshots/*/collect**", (route) => {
    calls.collect++;
    return route.fulfill({
      json: {
        linksFound: 1,
        linksAlreadyKnown: 0,
        linksFailed: 0,
        evidenceCreated: 0,
        aiStatus: "completed",
      },
    });
  });
  await page.route("**/v1/cv/evidence/*/review-state", async (route) => {
    const id = route.request().url().split("/").at(-2);
    const { reviewState } = route.request().postDataJSON();
    const item = evidence.find((entry) => entry.id === id);
    if (item) item.reviewState = reviewState;
    return route.fulfill({ json: item });
  });
  await page.route("**/v1/cv/generate", (route) => {
    calls.generate++;
    return route.fulfill({ status: 201, json: { id: "generation-1" } });
  });
  return calls;
}

async function clickGenerate(page: Page) {
  await page.goto("/");
  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await page.getByRole("tab", { name: "Colar Texto da Vaga" }).click();
  await page
    .getByRole("textbox", { name: "Texto da vaga" })
    .fill("Vaga fictícia para teste de revisão: desenvolvimento web.");
  await page
    .getByRole("button", { name: "Gerar currículo sob medida" })
    .click();
}

test("evidências novas pausam a geração num diálogo único e pendentes não bloqueiam", async ({
  page,
}) => {
  const calls = await mockAccount(page, evidenceFixture());
  await clickGenerate(page);

  const dialog = page.getByRole("dialog", {
    name: "Revise as evidências antes de gerar",
  });
  await expect(dialog).toBeVisible();
  // A busca roda no clique de gerar; a geração só segue depois da revisão.
  expect(calls.collect).toBe(1);
  expect(calls.generate).toBe(0);
  await expect(dialog.getByText("Usado por 300 alunos")).toBeVisible();
  await expect(
    dialog.getByRole("heading", { name: "Tech Lead — FinTech" }),
  ).toBeVisible();
  await expect(dialog.getByText("2 pendente(s)")).toBeVisible();

  // Confirmar uma e gerar sem decidir a outra: a pendente simplesmente fica de fora.
  await dialog.getByRole("button", { name: "Confirmar" }).first().click();
  await dialog.getByRole("button", { name: "Gerar currículo" }).click();
  await expect(page).toHaveURL(/\/generator$/);
  expect(calls.generate).toBe(1);
});

test("sem evidência pendente a geração segue direto, sem diálogo", async ({
  page,
}) => {
  const confirmedOnly = evidenceFixture().map((item) => ({
    ...item,
    reviewState: "confirmado" as const,
  }));
  const calls = await mockAccount(page, confirmedOnly);
  await clickGenerate(page);

  await expect(page).toHaveURL(/\/generator$/);
  expect(calls.collect).toBe(1);
  expect(calls.generate).toBe(1);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  // O card de evidências não faz mais parte das etapas do gerador.
  await expect(page.getByText("Analisar links")).toHaveCount(0);
});
