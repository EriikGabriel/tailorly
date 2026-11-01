import { expect, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const snapshotId = "66666666-6666-6666-6666-666666666666";

const evidence = [
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

test("evidências ficam num resumo e a lista completa abre num diálogo", async ({
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
  await page.route("**/v1/cv/evidence/snapshots/*", (route) =>
    route.fulfill({ json: evidence }),
  );
  await page.route("**/v1/owners/*/vault", (route) =>
    route.fulfill({
      json: {
        rootAvailable: true,
        webSearchAvailable: true,
        relativePath: "Vault",
        usable: true,
        readEnabled: true,
        exportEnabled: false,
        exportSubfolder: null,
        webSearchEnabled: true,
      },
    }),
  );
  await page.goto("/generator");
  await expect(
    page.getByText("Busca web ligada · Notas do vault (Vault)"),
  ).toBeVisible();
  await expect(page.getByText("Second Brain (Opcional")).toHaveCount(0);

  await expect(page.getByText("2 pendente(s)")).toBeVisible();
  // A lista longa não está na coluna: só no diálogo.
  await expect(page.getByText("Usado por 300 alunos")).toHaveCount(0);
  await page.getByRole("button", { name: "Revisar evidências" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("Usado por 300 alunos")).toBeVisible();
  await expect(
    dialog.getByRole("heading", { name: "Tech Lead — FinTech" }),
  ).toBeVisible();
  await expect(dialog.getByText("Second brain").first()).toBeVisible();
  await dialog.screenshot({ path: "/tmp/evidence-dialog.png" });
});
