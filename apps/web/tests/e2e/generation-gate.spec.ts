import { expect, type Page, test } from "@playwright/test";

const ownerId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const sessionId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const token = "tlr_anon_token-de-teste";
const versionId = "eeeeeeee-eeee-eeee-eeee-000000000001";
const documentId = "cccccccc-cccc-cccc-cccc-000000000001";

type RunStatus = "QUEUED" | "PROCESSING" | "COMPLETED" | "FAILED";

// Rotas /v1 não simuladas nunca devem alcançar uma API real que esteja no ar.
async function mockApi(page: Page, runs: { status: RunStatus | null }) {
  await page.route("**/v1/**", (route) =>
    route.fulfill({
      status: 404,
      json: { status: 404, message: "Rota não simulada." },
    }),
  );
  await page.route("**/v1/anonymous-sessions", (route) =>
    route.fulfill({
      status: 201,
      json: {
        sessionId,
        ownerId,
        token,
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      },
    }),
  );
  await page.route("**/v1/anonymous-sessions/current", (route) =>
    route.fulfill({
      json: {
        sessionId,
        ownerId,
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
        migratable: false,
        usage: { uploads: 0, extractions: 0, generations: 0 },
        limits: {
          maxFileBytes: 3145728,
          maxPages: 3,
          maxUploads: 3,
          maxExtractions: 3,
          maxGenerations: 3,
        },
      },
    }),
  );
  await page.route("**/v1/extraction-runs/version/*", (route) =>
    route.fulfill({
      json: runs.status
        ? [
            {
              id: "ffffffff-ffff-ffff-ffff-000000000001",
              baseVersionId: versionId,
              status: runs.status,
              errorCode: runs.status === "FAILED" ? "EXTRACTION_FAILED" : null,
              finishedAt: null,
              outputObjectId: null,
              pipelineFingerprint: "x",
              pipelineVersions: {},
              startedAt: null,
            },
          ]
        : [],
    }),
  );
  await page.route("**/v1/base-documents/upload?*", (route) =>
    route.fulfill({
      status: 201,
      json: {
        document: {
          id: documentId,
          ownerId,
          originalObjectId: "dddddddd-dddd-dddd-dddd-000000000001",
          contentHash: "hash-1",
          originalFilename: "curriculo-exemplo.pdf",
          pageCount: 1,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: versionId,
          ownerId,
          documentId,
          versionNumber: 1,
          status: "CANDIDATE",
          reviewRevision: 0,
          temporalReviewRequired: true,
          createdAt: new Date().toISOString(),
        },
        versionReused: false,
      },
    }),
  );
}

async function uploadBaseCv(page: Page) {
  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await expect(
    page.getByText("Arquivo enviado", { exact: true }),
  ).toBeVisible();
}

const generate = (page: Page) =>
  page.getByRole("button", {
    name: /Gerar currículo sob medida|Lendo e analisando/,
  });

test("a geração só libera com o Currículo Base extraído com sucesso e a vaga válida", {
  tag: "@generation-gate",
}, async ({ page }) => {
  const runs: { status: RunStatus | null } = { status: "QUEUED" };
  await mockApi(page, runs);
  await page.goto("/");

  await page.getByRole("tab", { name: "Colar Texto da Vaga" }).click();
  await page
    .getByRole("textbox", { name: "Texto da vaga" })
    .fill("Vaga fictícia: Experiência com Java.");

  // Sem Currículo Base.
  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Envie o Currículo Base para liberar a geração."),
  ).toBeVisible();

  // Enviado, mas na fila e depois processando: continua bloqueado.
  await uploadBaseCv(page);
  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Aguarde a extração do Currículo Base terminar."),
  ).toBeVisible();
  runs.status = "PROCESSING";
  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Aguarde a extração do Currículo Base terminar."),
  ).toBeVisible();

  // Concluída: o texto da vaga já basta.
  runs.status = "COMPLETED";
  await expect(generate(page)).toBeEnabled({ timeout: 10_000 });

  // No modo link, só um link válido libera.
  await page.getByRole("tab", { name: "Inserir URL da Vaga" }).click();
  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Informe o link da vaga para liberar a geração."),
  ).toBeVisible();

  const link = page.getByRole("textbox", { name: "Link da vaga" });
  await link.fill("acme.gupy.io/jobs/1");
  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Comece o link com https:// (ou http://).").first(),
  ).toBeVisible();

  await link.fill("https://www.linkedin.com/jobs/view/1");
  await expect(generate(page)).toBeDisabled();
  await expect(page.getByText(/Cole o texto da vaga/).first()).toBeVisible();

  await link.fill("http://127.0.0.1/vaga");
  await expect(generate(page)).toBeDisabled();

  await link.fill("https://acme.gupy.io/jobs/1");
  await expect(generate(page)).toBeEnabled();
});

test("extração com falha mantém a geração bloqueada mesmo com a vaga válida", {
  tag: "@generation-gate",
}, async ({ page }) => {
  await mockApi(page, { status: "FAILED" });
  await page.goto("/");
  await uploadBaseCv(page);
  await page
    .getByRole("textbox", { name: "Link da vaga" })
    .fill("https://acme.gupy.io/jobs/1");

  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText(
      "A extração do Currículo Base falhou. Envie o PDF novamente.",
    ),
  ).toBeVisible();
});

test("sem execução de extração ainda, a geração fica bloqueada", {
  tag: "@generation-gate",
}, async ({ page }) => {
  await mockApi(page, { status: null });
  await page.goto("/");
  await uploadBaseCv(page);
  await page
    .getByRole("textbox", { name: "Link da vaga" })
    .fill("https://acme.gupy.io/jobs/1");

  await expect(generate(page)).toBeDisabled();
  await expect(
    page.getByText("Aguarde o início da extração do Currículo Base."),
  ).toBeVisible();
});
