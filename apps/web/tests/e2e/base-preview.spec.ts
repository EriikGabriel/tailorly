import { readFileSync } from "node:fs";
import { expect, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";

test("a prévia do Currículo Base usa o mesmo visualizador nos dois modos, sem recarregar", {
  tag: "@base-preview",
}, async ({ page }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem(
      "tailorly:auth-session",
      JSON.stringify({
        accessToken: "token-de-teste",
        expiresAt: new Date(Date.now() + 60 * 60_000).toISOString(),
        refreshToken: "refresh-de-teste",
      }),
    );
  });
  await page.route("**/v1/**", (route) =>
    route.fulfill({
      status: 404,
      json: { status: 404, message: "Rota não simulada." },
    }),
  );
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
  await page.route("**/v1/base-documents/upload?*", (route) =>
    route.fulfill({
      status: 201,
      json: {
        document: {
          id: "33333333-3333-3333-3333-000000000001",
          ownerId,
          originalObjectId: "44444444-4444-4444-4444-000000000001",
          contentHash: "h1",
          originalFilename: "curriculo-exemplo.pdf",
          pageCount: null,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: "55555555-5555-5555-5555-000000000001",
          ownerId,
          documentId: "33333333-3333-3333-3333-000000000001",
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

  await page.goto("/base");
  await page.getByLabel("Selecionar PDF do Currículo Base").setInputFiles({
    name: "curriculo-exemplo.pdf",
    mimeType: "application/pdf",
    buffer: readFileSync("tests/fixtures/curriculo-exemplo.pdf"),
  });

  const viewer = page.locator(".react-pdf__Document");
  await expect(viewer.locator("canvas").first()).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator("iframe")).toHaveCount(0);

  const handle = await viewer.elementHandle();
  const canvasWidth = () =>
    viewer
      .locator("canvas")
      .first()
      .evaluate((canvas) => (canvas as HTMLCanvasElement).clientWidth);
  const fitWidth = await canvasWidth();

  // Ligar e desligar os pontos de extração só muda a sobreposição: o documento é o mesmo.
  await page.getByText("Bounding Boxes de IA", { exact: true }).click();
  await expect(page.getByText(/Pontos de extração da IA/)).toBeVisible();
  expect(await handle?.evaluate((element) => element.isConnected)).toBe(true);
  await page.getByText("Bounding Boxes de IA", { exact: true }).click();
  await expect(
    page.getByText("Documento original • Prévia local"),
  ).toBeVisible();
  expect(await handle?.evaluate((element) => element.isConnected)).toBe(true);

  // O zoom redimensiona as páginas no mesmo documento.
  await page.getByRole("button", { name: "Diminuir zoom" }).click();
  await expect.poll(canvasWidth).toBeLessThan(fitWidth);
  expect(await handle?.evaluate((element) => element.isConnected)).toBe(true);
  await expect(page.locator("iframe")).toHaveCount(0);
  await page.screenshot({ path: "test-results/base-preview.png" });
});
