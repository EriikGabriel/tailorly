import { expect, test } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const versionId = "55555555-5555-5555-5555-555555555555";
const jobPostingId = "66666666-6666-6666-6666-666666666666";

test(// O título vira parte do relatório e liga este cenário ao requisito funcional.
"RF16: bloquear a revisão até receber uma correção válida e diferente do original", {
  // Permite executar somente os cenários que cobrem RF16: `pnpm test:e2e --grep @RF16`.
  tag: "@RF16",
  annotation: {
    // A annotation aparece no relatório HTML como contexto de produto, não como
    // detalhe de implementação do teste.
    type: "requirement",
    description:
      "RF16 — diálogo de validação rápida: gate de completude antes de gerar",
  },
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
  // Nada fora do que o teste simula deve alcançar uma API real.
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
    route.fulfill({
      json: [
        {
          id: "rec-identity",
          baseVersionId: versionId,
          category: "identity",
          displayOrder: 0,
          facts: [
            {
              id: "fact-location",
              recordId: "rec-identity",
              fieldKey: "location",
              extractedValue: "São Paulo, Brasil",
              reviewedValue: null,
              origin: "ai",
              reviewState: "pendente",
              evidence: [],
            },
          ],
        },
      ],
    }),
  );
  // Uma única pendência real, sem relação com a vaga: a vaga não analisa (sem IA), então a
  // prova central deste teste (o gate de completude) não depende de priorização.
  await page.route("**/v1/review-issues/version/*", (route) =>
    route.fulfill({
      json: [
        {
          id: "issue-location",
          baseVersionId: versionId,
          factId: "fact-location",
          category: "identity",
          reason: "Confirme a localização extraída do documento.",
          blocking: false,
          status: "PENDING",
          questionAndChoices: null,
          fact: {
            id: "fact-location",
            recordId: "rec-identity",
            fieldKey: "location",
            extractedValue: "São Paulo, Brasil",
            reviewedValue: null,
            origin: "ai",
            reviewState: "pendente",
            evidence: [],
          },
        },
      ],
    }),
  );
  await page.route("**/v1/review-issues/*/answer", (route) =>
    route.fulfill({
      json: {
        id: "issue-location",
        baseVersionId: versionId,
        factId: "fact-location",
        category: "identity",
        reason: "Confirme a localização extraída do documento.",
        blocking: false,
        status: "ANSWERED",
        questionAndChoices: null,
        fact: null,
      },
    }),
  );
  await page.route("**/v1/job-postings", (route) =>
    route.fulfill({
      status: 201,
      json: {
        id: jobPostingId,
        jobPostingId,
        versionNumber: 1,
        contentHash: "job-hash",
        // Sem análise: a priorização por vaga fica desligada e todas as pendências pesam igual
        // — o ponto deste teste é o gate de completude, não a priorização.
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
  await page.route("**/v1/cv/generate", (route) =>
    route.fulfill({ status: 201, json: { id: "generation-1" } }),
  );

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

  // Limita os locators ao modal para que um botão de mesmo nome em outra parte
  // da página não satisfaça o teste por engano.
  const dialog = page.getByRole("dialog", {
    name: "Confirme 1 informação antes de gerar",
  });
  const generateButton = dialog.getByRole("button", {
    name: "Gerar currículo",
  });

  // A pendência real (não um dado fictício) aparece com a motivação registrada na API.
  await expect(dialog).toContainText(
    "Confirme a localização extraída do documento.",
  );
  await expect(generateButton).toBeDisabled();

  // Escolher "Corrigir" revela o campo de correção; vazio ainda deixa a pendência incompleta.
  await dialog.getByRole("button", { name: "Corrigir" }).click();
  const correction = dialog.getByLabel("Valor correto para Localização");
  await expect(correction).toBeEmpty();
  await expect(generateButton).toBeDisabled();

  // O valor original não é uma correção. A regra exige uma mudança factual.
  await correction.fill("São Paulo, Brasil");
  await expect(correction).toHaveAttribute("aria-invalid", "true");
  await expect(generateButton).toBeDisabled();

  // Um valor válido e diferente do original responde a única pendência e libera a geração.
  await correction.fill("Rio de Janeiro, Brasil");
  await expect(correction).toHaveAttribute("aria-invalid", "false");
  await expect(generateButton).toBeEnabled();

  // Regressão: apagar uma resposta antes válida deve restaurar o bloqueio.
  await correction.fill("");
  await expect(generateButton).toBeDisabled();

  // Com a pendência respondida de novo, o diálogo salva a resposta e gera o currículo.
  await correction.fill("Rio de Janeiro, Brasil");
  await generateButton.click();
  await expect(page).toHaveURL(/\/generator$/);
});
