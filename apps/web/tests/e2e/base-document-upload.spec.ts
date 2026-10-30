import { readFileSync } from "node:fs";
import { expect, test } from "@playwright/test";

test("RF09: envia PDF na home e reutiliza o fluxo em /base após reload", {
  tag: "@RF09",
  annotation: {
    type: "requirement",
    description:
      "RF09 — ingestão do Currículo Base na interface com API simulada",
  },
}, async ({ page }) => {
  const ownerId = "11111111-1111-1111-1111-111111111111";
  let uploads = 0;

  await page.addInitScript(() => {
    sessionStorage.setItem(
      "tailorly:auth-session",
      JSON.stringify({
        accessToken: "token-de-teste",
        expiresAt: new Date(Date.now() + 60 * 60_000).toISOString(),
      }),
    );
  });
  await page.route("**/v1/me", (route) =>
    route.fulfill({
      json: {
        owner: {
          id: ownerId,
          kind: "account",
          userId: "22222222-2222-2222-2222-222222222222",
          activeSnapshotId: null,
          expiresAt: null,
          revision: 0,
        },
        user: {
          id: "22222222-2222-2222-2222-222222222222",
          email: "teste@example.com",
          enabled: true,
          roles: ["ROLE_USER"],
        },
      },
    }),
  );
  await page.route("**/v1/base-documents/upload?*", async (route) => {
    expect(route.request().headers().authorization).toBe(
      "Bearer token-de-teste",
    );
    expect(route.request().url()).toContain(`ownerId=${ownerId}`);
    uploads += 1;
    await route.fulfill({
      status: 201,
      json: {
        document: {
          id: `33333333-3333-3333-3333-${String(uploads).padStart(12, "0")}`,
          ownerId,
          originalObjectId: `44444444-4444-4444-4444-${String(uploads).padStart(12, "0")}`,
          contentHash: `hash-${uploads}`,
          originalFilename:
            uploads === 1 ? "curriculo-exemplo.pdf" : "novo.pdf",
          pageCount: null,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: `55555555-5555-5555-5555-${String(uploads).padStart(12, "0")}`,
          ownerId,
          documentId: `33333333-3333-3333-3333-${String(uploads).padStart(12, "0")}`,
          versionNumber: uploads,
          status: "CANDIDATE",
          reviewRevision: 0,
          temporalReviewRequired: true,
          createdAt: new Date().toISOString(),
        },
        versionReused: false,
      },
    });
  });

  await page.goto("/");
  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await expect(
    page.getByText("Arquivo enviado", { exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(page.getByLabel("Selecionar currículo base")).toBeVisible();

  await page.goto("/base");
  const nextPdf = readFileSync("tests/fixtures/curriculo-exemplo.pdf");
  await page.getByLabel("Selecionar PDF do Currículo Base").setInputFiles({
    name: "novo.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.concat([nextPdf, Buffer.from("\n% novo arquivo")]),
  });
  await expect(page.getByText("PDF enviado", { exact: true })).toBeVisible();
  expect(uploads).toBe(2);
});
