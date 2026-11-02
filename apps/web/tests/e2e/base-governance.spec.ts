import { expect, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const versionId = "55555555-5555-5555-5555-555555555555";

const fact = (
  id: string,
  reviewState: "pendente" | "confirmado" | "omitido",
  withEvidence: boolean,
) => ({
  id,
  recordId: "rec-1",
  fieldKey: `campo-${id}`,
  extractedValue: `valor ${id}`,
  reviewedValue: null,
  origin: "ai",
  reviewState,
  evidence: withEvidence
    ? [
        {
          id: `ev-${id}`,
          extractedPageId: "page-1",
          sourceExcerpt: "trecho",
          sourceLocator: null,
        },
      ]
    : [],
});

test("índice de confiabilidade e protocolo de integridade refletem os fatos reais", async ({
  page,
}) => {
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
          activeSnapshotId: null,
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
  // 4 fatos: 1 omitido (não conta), 1 confirmado com evidência, 1 confirmado sem, 1 pendente.
  await page.route("**/v1/fact-records/version/*", (route) =>
    route.fulfill({
      json: [
        {
          id: "rec-1",
          baseVersionId: versionId,
          category: "identity",
          displayOrder: 0,
          facts: [
            fact("a", "confirmado", true),
            fact("b", "confirmado", false),
            fact("c", "pendente", true),
            fact("d", "omitido", true),
          ],
        },
      ],
    }),
  );
  await page.route("**/v1/review-issues/version/*", (route) =>
    route.fulfill({
      json: [
        {
          id: "issue-c",
          baseVersionId: versionId,
          factId: "c",
          category: "identity",
          reason: "Confirme este fato.",
          blocking: true,
          status: "PENDING",
          questionAndChoices: null,
          fact: fact("c", "pendente", true),
        },
      ],
    }),
  );

  await page.goto("/");
  const uploaded = page.waitForResponse("**/v1/base-documents/upload?*");
  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await uploaded;
  // Navegação dentro do app, para manter a versão enviada em memória.
  await page.getByRole("link", { name: "Currículo Base" }).first().click();
  await expect(page).toHaveURL(/\/base$/);

  // Cada cartão é localizado pelo próprio título, para não casar texto de outras partes da página.
  const reliability = page
    .getByRole("heading", { name: "Índice de confiabilidade" })
    .locator("xpath=..");
  const integrity = page
    .getByRole("heading", { name: "Protocolo de integridade" })
    .locator("xpath=..");

  // 2 de 3 fatos considerados revisados (o omitido não conta).
  // A rota /base é compilada sob demanda pelo Vite; com vários workers a primeira carga é lenta.
  await expect(reliability.getByText("67%")).toBeVisible({ timeout: 20_000 });
  await expect(
    reliability.getByText("2 de 3 fatos revisados"),
  ).toBeVisible();
  await expect(integrity.getByText("1 pendência obrigatória")).toBeVisible();
  await expect(
    integrity.getByText("Resolva antes de aprovar o currículo base"),
  ).toBeVisible();
});
