import { readFileSync } from "node:fs";
import { expect, type Page, test } from "@playwright/test";

const ownerId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const sessionId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const token = "tlr_anon_token-de-teste";
const generationId = "99999999-9999-9999-9999-000000000001";

const pdfA = readFileSync("tests/fixtures/preview-a.pdf");
const pdfB = readFileSync("tests/fixtures/preview-b.pdf");

const TEMPLATE = [
  '#let accent = rgb("#0f4c5c")',
  "#let lines(text) = text",
  "",
  '#set page(paper: "a4", margin: (x: 1.8cm, y: 1.6cm))',
  "",
  "#show heading.where(level: 2): it => block(above: 1em, width: 100%)[",
  "  #text(fill: accent, it.body)",
  "  #line(length: 100%)",
  "]",
  "",
  "// tailorly:content",
].join("\n");

const generation = {
  id: generationId,
  ownerId,
  createdAt: new Date().toISOString(),
  reused: false,
  groundTruth: {
    snapshotId: "s-1",
    snapshotNumber: 1,
    baseVersionId: "v-1",
    contentHash: "h1",
  },
  job: {
    jobPostingId: "j-1",
    versionId: "jv-1",
    versionNumber: 1,
    title: "Engenheira de Software",
    companyName: "Acme",
    contentHash: "h2",
  },
  template: {
    id: "t-1",
    name: "classic",
    version: 1,
    custom: false,
    content: TEMPLATE,
  },
  ats: {
    aiStatus: "skipped",
    iterations: [],
    report: {
      score: 82,
      pages: 1,
      words: 300,
      checks: [],
      matchedTerms: [],
      missingTerms: [],
    },
  },
  pdf: {
    format: "pdf",
    mimeType: "application/pdf",
    sizeBytes: 1000,
    contentHash: "p1",
  },
  markdown: {
    format: "markdown",
    mimeType: "text/markdown",
    sizeBytes: 100,
    contentHash: "m1",
  },
  matchedTerms: [],
  preferences: { guidelines: [], optimizeForAts: true },
};

type Previews = { contents: string[] };

async function mockApi(page: Page, previews: Previews) {
  await page.addInitScript(
    ([sid, owner, tok]) => {
      sessionStorage.setItem(
        "tailorly:anonymous-session",
        JSON.stringify({
          token: tok,
          sessionId: sid,
          ownerId: owner,
          expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
        }),
      );
    },
    [sessionId, ownerId, token],
  );
  await page.route("**/v1/**", (route) =>
    route.fulfill({
      status: 404,
      json: { status: 404, message: "Rota não simulada." },
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
  await page.route(`**/v1/cv/generations/owner/${ownerId}`, (route) =>
    route.fulfill({ json: [generation] }),
  );
  await page.route(`**/v1/cv/generations/${generationId}`, (route) =>
    route.fulfill({ json: generation }),
  );
  await page.route(`**/v1/cv/generations/${generationId}/preview`, (route) => {
    const content = (route.request().postDataJSON() as { content: string })
      .content;
    previews.contents.push(content);
    if (content.includes("accnt")) {
      return route.fulfill({
        status: 422,
        json: {
          status: 422,
          message: "Não foi possível compilar o template",
          details: [
            "error: unknown variable: accnt",
            "  ┌─ template.typ:1:13",
            "  │",
            "1 │ #text(fill: accnt)[x]",
            "  │             ^^^^^",
          ],
        },
      });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/pdf",
      body: content.includes("VERSAO_B") ? pdfB : pdfA,
    });
  });
}

async function openSplitEditor(page: Page) {
  await page.goto("/");
  await page.getByRole("link", { name: "Abrir Editor" }).first().click();
  await page.getByRole("tab", { name: "Dividido" }).click();
  await expect(page.getByTestId("split-handle")).toBeVisible({
    timeout: 20_000,
  });
  // O primeiro desenho carrega o worker do pdf.js, que o servidor de desenvolvimento ainda precisa transformar.
  await expect(page.getByTestId("pdf-pages").locator("canvas")).toHaveCount(1, {
    timeout: 30_000,
  });
}

const code = (page: Page) =>
  page.getByTestId("typst-code").locator(".cm-content");

async function widthOf(page: Page, testId: string) {
  const box = await page.getByTestId(testId).boundingBox();
  if (!box) throw new Error(`${testId} não está visível`);
  return box.width;
}

test.use({ viewport: { width: 1440, height: 900 } });

test("o divisor arrasta, responde ao teclado, volta ao centro e lembra a proporção", {
  tag: "@generator-editor",
}, async ({ page }) => {
  await mockApi(page, { contents: [] });
  await openSplitEditor(page);
  const handle = page.getByTestId("split-handle");

  const before = await widthOf(page, "split-first");
  const box = await handle.boundingBox();
  if (!box) throw new Error("divisor sem posição");
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2 + 220, box.y + box.height / 2, {
    steps: 10,
  });
  await page.mouse.up();

  const after = await widthOf(page, "split-first");
  expect(after).toBeGreaterThan(before + 150);
  expect(Number(await handle.getAttribute("aria-valuenow"))).toBeGreaterThan(
    55,
  );
  await page.screenshot({ path: "test-results/generator-split-dragged.png" });

  await handle.focus();
  await page.keyboard.press("Home");
  await expect(handle).toHaveAttribute("aria-valuenow", "25");
  await page.keyboard.press("End");
  await expect(handle).toHaveAttribute("aria-valuenow", "75");
  await page.keyboard.press("ArrowLeft");
  await expect(handle).toHaveAttribute("aria-valuenow", "73");

  await handle.dblclick();
  await expect(handle).toHaveAttribute("aria-valuenow", "50");

  // O limite impede esconder um dos painéis.
  await page.keyboard.press("Home");
  await page.keyboard.press("ArrowLeft");
  await expect(handle).toHaveAttribute("aria-valuenow", "25");

  // A proporção fica no navegador: ao reabrir o editor, continua em 25.
  await page.goto("/");
  await page.getByRole("link", { name: "Abrir Editor" }).first().click();
  await page.getByRole("tab", { name: "Dividido" }).click();
  await expect(page.getByTestId("split-handle")).toHaveAttribute(
    "aria-valuenow",
    "25",
  );
});

test("a prévia troca o PDF no lugar, sem recarregar o visualizador", {
  tag: "@generator-editor",
}, async ({ page }) => {
  const previews: Previews = { contents: [] };
  await mockApi(page, previews);
  await openSplitEditor(page);

  await expect(page.locator("iframe")).toHaveCount(0);
  const pages = page.getByTestId("pdf-pages");
  const host = await pages.elementHandle();
  const swapsBefore = Number(await pages.getAttribute("data-swaps"));
  const snapshot = () =>
    pages
      .locator("canvas")
      .first()
      .evaluate((c) => (c as HTMLCanvasElement).toDataURL());
  const bitmapA = await snapshot();
  const requestsBefore = previews.contents.length;

  await code(page).click();
  await page.keyboard.press("Control+End");
  await page.keyboard.type("\n// VERSAO_B");

  await expect
    .poll(() => previews.contents.length)
    .toBeGreaterThan(requestsBefore);
  await expect
    .poll(async () => Number(await pages.getAttribute("data-swaps")), {
      timeout: 10_000,
    })
    .toBeGreaterThan(swapsBefore);
  await expect.poll(snapshot, { timeout: 10_000 }).not.toBe(bitmapA);

  // O mesmo elemento continua na página (nada foi desmontado) e ainda não há iframe.
  expect(await host?.evaluate((element) => element.isConnected)).toBe(true);
  await expect(page.locator("iframe")).toHaveCount(0);
  await expect(page.getByTestId("preview-status")).toContainText("Atualizado");
  await expect(page.getByTestId("preview-pages")).toHaveText("Página 1 de 1");
});

test("a barra de ferramentas, os atalhos e o autocompletar editam o código", {
  tag: "@generator-editor",
}, async ({ page }) => {
  const previews: Previews = { contents: [] };
  await mockApi(page, previews);
  await openSplitEditor(page);

  // Há uma amostra de cor para o rgb("#0f4c5c") do template.
  await expect(page.locator(".cm-color-swatch")).toHaveCount(1);

  await code(page).click();
  await page.keyboard.press("Control+End");
  await page.getByRole("button", { name: "Negrito" }).click();
  await expect(code(page)).toContainText("*negrito*");

  // Depois de escolher um trecho no menu, o foco volta ao editor e os atalhos continuam valendo.
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("Enter");
  await page.getByRole("button", { name: "Inserir trecho de Typst" }).click();
  await page.getByRole("menuitem", { name: "Espaço vertical" }).click();
  await expect(code(page)).toContainText("#v(0.5em)");
  await expect(code(page)).toBeFocused();

  // Comentar a linha atual com o atalho.
  await page.keyboard.press("Control+/");
  await expect(code(page)).toContainText("// #v(0.5em)");
  await page.keyboard.press("Control+/");
  await expect(code(page)).not.toContainText("// #v(0.5em)");

  // Dobrar e expandir todos os blocos (o bloco do show heading tem várias linhas).
  await page.getByRole("button", { name: "Recolher todos os blocos" }).click();
  await expect(page.locator(".cm-foldPlaceholder").first()).toBeVisible();
  await page.getByRole("button", { name: "Expandir todos os blocos" }).click();
  await expect(page.locator(".cm-foldPlaceholder")).toHaveCount(0);

  // Autocompletar depois de "#": receitas do projeto e funções da gramática.
  await code(page).click();
  await page.keyboard.press("Control+End");
  await page.keyboard.type("\n#se");
  await expect(page.locator(".cm-tooltip-autocomplete")).toContainText(
    "set text",
  );
  await page.keyboard.press("Escape");

  // Ctrl+Enter compila na hora, mesmo sem mudar o código.
  const requests = previews.contents.length;
  await expect(page.getByTestId("preview-status")).toContainText("Atualizado");
  await page.keyboard.press("Control+Enter");
  await expect.poll(() => previews.contents.length).toBeGreaterThan(requests);

  await page.screenshot({ path: "test-results/generator-split-toolbar.png" });
});

test("erros do compilador viram marcas no código e somem ao corrigir", {
  tag: "@generator-editor",
}, async ({ page }) => {
  await mockApi(page, { contents: [] });
  await openSplitEditor(page);

  await code(page).click();
  await page.keyboard.press("Control+Home");
  await page.keyboard.type("#text(fill: accnt)[x]\n");

  await expect(page.getByTestId("preview-status")).toContainText(
    "Erro de compilação",
    { timeout: 10_000 },
  );
  await expect(page.locator(".cm-lintRange-error")).toHaveCount(1);
  await expect(page.getByTestId("editor-issues")).toHaveText(
    "1 erro · 0 avisos",
  );
  // O último PDF válido continua na tela.
  await expect(page.getByTestId("pdf-pages").locator("canvas")).toHaveCount(1);
  await page.screenshot({ path: "test-results/generator-split-error.png" });

  await page.keyboard.press("Control+Home");
  for (let i = 0; i < "#text(fill: accnt)[x]\n".length; i++)
    await page.keyboard.press("Delete");

  await expect(page.getByTestId("preview-status")).toContainText("Atualizado", {
    timeout: 10_000,
  });
  await expect(page.locator(".cm-lintRange-error")).toHaveCount(0);
  await expect(page.getByTestId("editor-issues")).toHaveText("Sem erros");
});
