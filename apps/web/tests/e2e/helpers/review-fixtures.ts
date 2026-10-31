import { expect, type Page } from "@playwright/test";

const ownerId = "11111111-1111-1111-1111-111111111111";
const versionId = "55555555-5555-5555-5555-555555555555";

type FactSeed = [key: string, value: string];

function record(category: string, order: number, facts: FactSeed[]) {
  const recordId = `rec-${category}`;
  return {
    id: recordId,
    baseVersionId: versionId,
    category,
    displayOrder: order,
    facts: facts.map(([fieldKey, value], index) => ({
      id: `${category}-${index}`,
      recordId,
      fieldKey,
      extractedValue: value,
      reviewedValue: null,
      origin: "ai",
      reviewState: "pendente",
      evidence: [],
    })),
  };
}

const records = [
  record("identity", 0, [
    ["name", "Erik Gabriel"],
    ["headline", "Engenheiro de Software Pleno"],
    ["location", "São Paulo, Brasil"],
    ["email", "erik@example.com"],
    ["phone", "(11) 98765-4321"],
    ["linkedin", "linkedin.com/in/erikgabriel"],
    ["github", "github.com/EriikGabriel"],
    ["website", "erikgabriel.vercel.app"],
  ]),
  record("experience", 1, [
    ["0:company", "FinTech Solutions Brasil"],
    ["0:role", "Tech Lead"],
    ["0:employment", "CLT"],
    ["0:start", "2022-01"],
    ["0:status", "Atual"],
    ["1:company", "HealthTech Corp"],
    ["1:role", "Engenheiro de Software"],
    ["1:start", "2019-03"],
    ["1:end", "2021-12"],
    ["1:status", "Encerrado"],
  ]),
  record("volunteering", 2, [
    ["0:organization", "ONG Educar"],
    ["0:role", "Instrutor de programação"],
    ["0:start", "2020-03"],
    ["0:end", "2021-06"],
    ["0:status", "Encerrado"],
    ["0:description", "Aulas de lógica de programação para jovens."],
  ]),
  record("degrees", 2, [
    ["0:degree", "Bacharelado em Ciência da Computação"],
    ["0:institution", "UFSCar"],
    ["0:start", "2015"],
    ["0:end", "2019"],
    ["0:status", "Concluído"],
  ]),
  record("projects", 2, [
    ["0:title", "UFSCar Planner"],
    ["0:context", "Projeto acadêmico"],
    ["0:statement", "Aplicativo de planejamento acadêmico usado por alunos."],
    ["0:metric", "300 alunos"],
    ["0:link", "github.com/EriikGabriel/ufscar-planner"],
  ]),
  record("languages", 3, [
    ["0:language", "Inglês"],
    ["0:proficiency", "avançado"],
    ["1:language", "Espanhol"],
    ["1:proficiency", "B2"],
  ]),
  record("technologies", 4, [
    ["0:name", "Java"],
    ["1:name", "Spring Boot"],
  ]),
];

export async function openReview(page: Page) {
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
  await page.route("**/v1/extracted-pages/run/*", (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route("**/v1/fact-records/version/*", (route) =>
    route.fulfill({ json: records }),
  );
  await page.route("**/v1/review-issues/version/*", (route) =>
    route.fulfill({ json: [] }),
  );

  await page.goto("/base");
  await page
    .getByLabel("Selecionar PDF do Currículo Base")
    .setInputFiles("tests/fixtures/curriculo-exemplo.pdf");
  await expect(
    page.getByText("PDF enviado", { exact: true }).first(),
  ).toBeVisible();
}

export const step = (page: Page, name: string) =>
  page
    .getByRole("navigation", { name: "Etapas de revisão do Currículo Base" })
    .getByRole("button", { name });
