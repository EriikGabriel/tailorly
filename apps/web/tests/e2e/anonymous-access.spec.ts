import { expect, type Page, test } from "@playwright/test";

const ownerId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const sessionId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const token = "tlr_anon_token-de-teste";

// Rotas /v1 não simuladas nunca devem alcançar uma API real que esteja no ar: um 401
// dela descartaria a sessão de teste. As rotas específicas, registradas depois, têm prioridade.
async function blockUnmockedApi(page: Page) {
  await page.route("**/v1/**", (route) =>
    route.fulfill({
      status: 404,
      json: { status: 404, message: "Rota não simulada no teste." },
    }),
  );
}

const expiresAt = () => new Date(Date.now() + 60 * 60_000).toISOString();

const status = (overrides: Record<string, unknown> = {}) => ({
  sessionId,
  ownerId,
  expiresAt: expiresAt(),
  migratable: false,
  usage: { uploads: 0, extractions: 0, generations: 0 },
  limits: {
    maxFileBytes: 3 * 1024 * 1024,
    maxPages: 3,
    maxUploads: 3,
    maxExtractions: 3,
    maxGenerations: 3,
  },
  ...overrides,
});

test("RF07: visitante envia o PDF sem conta e a sessão anônima é emitida sob demanda", {
  tag: "@RF07",
  annotation: {
    type: "requirement",
    description:
      "RF07 — uso anônimo: a sessão é emitida pelo servidor e usada como credencial",
  },
}, async ({ page }) => {
  let sessions = 0;
  let uploads = 0;

  await blockUnmockedApi(page);
  await page.route("**/v1/anonymous-sessions", async (route) => {
    sessions += 1;
    await route.fulfill({
      status: 201,
      json: { sessionId, ownerId, token, expiresAt: expiresAt() },
    });
  });
  await page.route("**/v1/anonymous-sessions/current", (route) =>
    route.fulfill({ json: status() }),
  );
  await page.route("**/v1/extraction-runs/version/*", (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route("**/v1/base-documents/upload?*", async (route) => {
    // O visitante usa o token anônimo e o owner da sessão, nunca um JWT de conta.
    expect(route.request().headers().authorization).toBe(`Bearer ${token}`);
    expect(route.request().url()).toContain(`ownerId=${ownerId}`);
    uploads += 1;
    await route.fulfill({
      status: 201,
      json: {
        document: {
          id: `cccccccc-cccc-cccc-cccc-${String(uploads).padStart(12, "0")}`,
          ownerId,
          originalObjectId: `dddddddd-dddd-dddd-dddd-${String(uploads).padStart(12, "0")}`,
          contentHash: `hash-${uploads}`,
          originalFilename: "curriculo-exemplo.pdf",
          pageCount: 1,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: `eeeeeeee-eeee-eeee-eeee-${String(uploads).padStart(12, "0")}`,
          ownerId,
          documentId: `cccccccc-cccc-cccc-cccc-${String(uploads).padStart(12, "0")}`,
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
  await expect(page.getByText("Modo visitante:")).toBeVisible();
  // Nada é emitido só por abrir a página.
  expect(sessions).toBe(0);

  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await expect(
    page.getByText("Arquivo enviado", { exact: true }),
  ).toBeVisible();
  expect(sessions).toBe(1);
  await expect(
    page.getByText(/Limites: PDF de até 3 MiB e 3 páginas/),
  ).toBeVisible();

  const stored = await page.evaluate(() =>
    sessionStorage.getItem("tailorly:anonymous-session"),
  );
  expect(stored).toContain(token);

  // Um segundo envio reaproveita a mesma sessão.
  await page.getByText("Trocar").click();
  await page.getByLabel("Selecionar currículo base").setInputFiles({
    name: "outro.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n% outro arquivo"),
  });
  await expect.poll(() => uploads).toBe(2);
  expect(sessions).toBe(1);
});

test("RF07: o servidor recusa o envio do visitante e a mensagem localizada é exibida", {
  tag: "@RF07",
}, async ({ page }) => {
  await blockUnmockedApi(page);
  await page.route("**/v1/anonymous-sessions", (route) =>
    route.fulfill({
      status: 201,
      json: { sessionId, ownerId, token, expiresAt: expiresAt() },
    }),
  );
  await page.route("**/v1/anonymous-sessions/current", (route) =>
    route.fulfill({ json: status() }),
  );
  await page.route("**/v1/base-documents/upload?*", (route) =>
    route.fulfill({
      status: 400,
      json: {
        timestamp: new Date().toISOString(),
        status: 400,
        error: "Requisição inválida",
        message:
          "Uploads anônimos têm limite de páginas. Crie uma conta para enviar arquivos mais longos",
        details: null,
        path: "/v1/base-documents/upload",
      },
    }),
  );

  await page.goto("/");
  await page
    .getByLabel("Selecionar currículo base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");

  await expect(page.getByRole("alert")).toContainText(
    "Uploads anônimos têm limite de páginas",
  );
});

test.describe("migração após o login", () => {
  test.beforeEach(async ({ page }) => {
    await blockUnmockedApi(page);
    await page.addInitScript(
      ([anonymousToken, id, owner]) => {
        const later = new Date(Date.now() + 60 * 60_000).toISOString();
        sessionStorage.setItem(
          "tailorly:auth-session",
          JSON.stringify({ accessToken: "jwt-de-teste", expiresAt: later }),
        );
        sessionStorage.setItem(
          "tailorly:anonymous-session",
          JSON.stringify({
            token: anonymousToken,
            sessionId: id,
            ownerId: owner,
            expiresAt: later,
          }),
        );
      },
      [token, sessionId, ownerId],
    );
    await page.route("**/v1/me", (route) =>
      route.fulfill({
        json: {
          owner: {
            id: "11111111-1111-1111-1111-111111111111",
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
    await page.route("**/v1/anonymous-sessions/current", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 204 });
      }
      return route.fulfill({ json: status({ migratable: true }) });
    });
  });

  test("oferece trazer o currículo aprovado como visitante e pede confirmação para substituir", {
    tag: "@RF07",
  }, async ({ page }) => {
    const bodies: Array<{ sessionToken: string; replaceActive: boolean }> = [];
    await page.route("**/v1/anonymous-sessions/migrate", async (route) => {
      // A conta é quem migra; o token anônimo vai só no corpo.
      expect(route.request().headers().authorization).toBe(
        "Bearer jwt-de-teste",
      );
      bodies.push(route.request().postDataJSON());
      if (bodies.length === 1) {
        await route.fulfill({
          status: 409,
          json: {
            timestamp: new Date().toISOString(),
            status: 409,
            error: "Conflito",
            message: "Sua conta já tem um Ground Truth ativo.",
            details: null,
            path: "/v1/anonymous-sessions/migrate",
          },
        });
        return;
      }
      await route.fulfill({
        json: {
          snapshotId: "99999999-9999-9999-9999-999999999999",
          baseVersionId: "88888888-8888-8888-8888-888888888888",
          documentId: "77777777-7777-7777-7777-777777777777",
          documentReused: false,
          replacedActive: true,
        },
      });
    });

    await page.goto("/");
    const dialog = page.getByRole("dialog", {
      name: "Trazer seu currículo para a conta?",
    });
    await expect(dialog).toBeVisible();

    await dialog
      .getByRole("button", { name: "Trazer para minha conta" })
      .click();
    await expect(dialog).toContainText(
      "Sua conta já tem um Ground Truth ativo",
    );
    expect(bodies[0]).toEqual({ sessionToken: token, replaceActive: false });

    await dialog.getByRole("button", { name: "Substituir e trazer" }).click();
    await expect(dialog).toBeHidden();
    expect(bodies[1]).toEqual({ sessionToken: token, replaceActive: true });
    expect(
      await page.evaluate(() =>
        sessionStorage.getItem("tailorly:anonymous-session"),
      ),
    ).toBeNull();
  });

  test("descartar apaga a sessão de visitante no servidor e no cliente", {
    tag: "@RF07",
  }, async ({ page }) => {
    let deleted = false;
    await page.route("**/v1/anonymous-sessions/current", (route) => {
      if (route.request().method() === "DELETE") {
        expect(route.request().headers().authorization).toBe(`Bearer ${token}`);
        deleted = true;
        return route.fulfill({ status: 204 });
      }
      return route.fulfill({ json: status({ migratable: true }) });
    });

    await page.goto("/");
    const dialog = page.getByRole("dialog", {
      name: "Trazer seu currículo para a conta?",
    });
    await dialog
      .getByRole("button", { name: "Descartar currículo de visitante" })
      .click();

    await expect(dialog).toBeHidden();
    expect(deleted).toBe(true);
    expect(
      await page.evaluate(() =>
        sessionStorage.getItem("tailorly:anonymous-session"),
      ),
    ).toBeNull();
  });
});
