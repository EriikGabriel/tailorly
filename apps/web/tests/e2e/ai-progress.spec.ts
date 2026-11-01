import { expect, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const versionId = "55555555-5555-5555-5555-555555555555";

test("extração mostra a etapa atual e o que a IA está fazendo", async ({
  page,
}) => {
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
          id: "33333333-3333-3333-3333-333333333333",
          ownerId,
          originalObjectId: "44444444-4444-4444-4444-444444444444",
          contentHash: "h",
          originalFilename: "curriculo-exemplo.pdf",
          pageCount: 2,
          createdAt: new Date().toISOString(),
        },
        reused: false,
        version: {
          id: versionId,
          ownerId,
          documentId: "33333333-3333-3333-3333-333333333333",
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
  await page.route("**/v1/extraction-runs/version/*", (route) =>
    route.fulfill({
      json: [
        {
          id: "run-1",
          baseVersionId: versionId,
          status: "PROCESSING",
          stage: "INTERPRETING",
          stageDetail: null,
          startedAt: new Date(Date.now() - 14_000).toISOString(),
          finishedAt: null,
          errorCode: null,
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

  await page.goto("/base");
  await page
    .getByLabel("Selecionar PDF do Currículo Base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");

  const status = page.getByText("A IA separa os fatos do currículo", {
    exact: true,
  });
  await expect(status).toBeVisible();
  await status.screenshot({ path: "/tmp/ai-progress-extraction.png" });
});
