import { expect, test } from "@playwright/test";
import { openReview, step } from "./helpers/review-fixtures";

const popups = (page: import("@playwright/test").Page) =>
  page.locator('[data-slot="popover-popup"]');

test("RF10: identificação mostra prévia, ícones e máscara de telefone", {
  tag: "@RF10",
}, async ({ page }) => {
  await openReview(page);

  const panel = page.locator("#review-active-panel");
  await expect(panel.getByText("Erik Gabriel").first()).toBeVisible();
  // Cartão de prévia com avatar de iniciais e links formatados como domínio/caminho.
  await expect(panel.getByText("EG", { exact: true })).toBeVisible();
  await expect(
    panel.getByRole("link", { name: "github.com/EriikGabriel", exact: true }),
  ).toHaveAttribute("href", "https://github.com/EriikGabriel");

  // A máscara formata o telefone enquanto digita.
  const phone = panel.getByRole("textbox", { name: "Telefone / WhatsApp" });
  await phone.fill("11912345678");
  await expect(phone).toHaveValue("(11) 91234-5678");

  // E-mail inválido é sinalizado.
  const email = panel.getByRole("textbox", { name: "E-mail", exact: true });
  await email.fill("erik@");
  await expect(email).toHaveAttribute("aria-invalid", "true");
});

test("RF10: experiência usa calendário, interruptor 'Atualmente' e lista de vínculo", {
  tag: "@RF10",
}, async ({ page }) => {
  await openReview(page);
  await step(page, "Experiência").click();

  const panel = page.locator("#review-active-panel");
  // Cabeçalho resumido e formatado, sem texto cru.
  const first = panel.getByRole("button", { name: /Tech Lead/ });
  await expect(first).toContainText("jan 2022 — atual");
  await expect(first).toContainText("CLT");
  await first.click();

  // O interruptor está ligado: o término fica desabilitado.
  const current = panel
    .getByRole("switch", { name: "Trabalho aqui atualmente" })
    .first();
  await expect(current).toBeChecked();
  await expect(
    panel.getByRole("button", { name: /^Término:/ }).first(),
  ).toBeDisabled();

  // Calendário: escolhe outro mês de início.
  await panel
    .getByRole("button", { name: /^Início: jan 2022/ })
    .first()
    .click();
  await page.getByRole("button", { name: "mar", exact: true }).click();
  await expect(popups(page)).toHaveCount(0);
  await expect(first).toContainText("mar 2022 — atual");

  // Desliga "atualmente" e define o término; a situação vira "Encerrado".
  await current.click();
  await panel
    .getByRole("button", { name: /^Término:/ })
    .first()
    .click();
  await page.getByRole("button", { name: "dez", exact: true }).click();
  await expect(popups(page)).toHaveCount(0);
  await expect(first).toContainText("mar 2022 — dez");
  await expect(first).toContainText("Encerrado");

  // Término anterior ao início é sinalizado.
  await panel
    .getByRole("button", { name: /^Término:/ })
    .first()
    .click();
  await page.getByRole("button", { name: "Ano anterior" }).click();
  await page.getByRole("button", { name: "jan", exact: true }).click();
  await expect(popups(page)).toHaveCount(0);
  await expect(panel.getByRole("alert")).toContainText(
    "O término é anterior ao início.",
  );
});

test("RF10: formação aceita só o ano e idiomas usam níveis em degraus", {
  tag: "@RF10",
}, async ({ page }) => {
  await openReview(page);

  await step(page, "Formação").click();
  const panel = page.locator("#review-active-panel");
  const degree = panel.getByRole("button", { name: /Bacharelado/ });
  await expect(degree).toContainText("2015 — 2019");
  await expect(degree).toContainText("Concluído");

  await step(page, "Competências").click();
  // "avançado" é reconhecido; "B2" não é, e o valor original continua visível na tela.
  const english = panel.getByRole("group", { name: "Nível de Inglês" });
  const spanish = panel.getByRole("group", { name: "Nível de Espanhol" });
  await expect(
    english.getByRole("button", { name: "Avançado" }),
  ).toHaveAttribute("aria-pressed", "true");
  await expect(panel.getByText("Valor do documento:")).toContainText("B2");
  await spanish.getByRole("button", { name: "Intermediário" }).click();
  await expect(panel.getByText("Valor do documento:")).toHaveCount(0);
  await expect(
    spanish.getByRole("button", { name: "Intermediário" }),
  ).toHaveAttribute("aria-pressed", "true");

  // Chips: adiciona várias de uma vez e remove uma.
  const add = panel.getByPlaceholder("Adicionar tecnologia (Enter)");
  await add.fill("Docker, Kubernetes");
  await add.press("Enter");
  await expect(
    panel.getByRole("button", { name: "Editar contexto de Docker" }),
  ).toBeVisible();
  await expect(
    panel.getByRole("button", { name: "Editar contexto de Kubernetes" }),
  ).toBeVisible();
  // Duplicata (sem diferenciar maiúsculas) não é adicionada de novo.
  await add.fill("docker");
  await add.press("Enter");
  await expect(
    panel.getByRole("button", { name: /Editar contexto de docker/i }),
  ).toHaveCount(1);
  await panel.getByRole("button", { name: "Remover Java" }).click();
  await expect(
    panel.getByRole("button", { name: "Editar contexto de Java" }),
  ).toHaveCount(0);
});

test("RF10: voluntariado fica separado do histórico profissional", {
  tag: "@RF10",
}, async ({ page }) => {
  await openReview(page);
  await step(page, "Experiência").click();

  const panel = page.locator("#review-active-panel");
  const professional = panel.getByRole("region", {
    name: /Histórico profissional/,
  });
  const volunteering = panel.getByRole("region", {
    name: /Atividades voluntárias/,
  });

  // Cada seção só tem os seus próprios cartões.
  await expect(
    professional.getByRole("button", { name: /Tech Lead/ }),
  ).toBeVisible();
  await expect(professional.getByText("ONG Educar")).toHaveCount(0);
  await expect(volunteering.getByText("Tech Lead")).toHaveCount(0);

  // Cartão de voluntariado com resumo formatado e situação.
  const card = volunteering.getByRole("button", {
    name: /Instrutor de programação/,
  });
  await expect(card).toContainText("ONG Educar");
  await expect(card).toContainText("mar 2020 — jun 2021");
  await expect(card).toContainText("Encerrado");
  await card.click();

  // Campos próprios do voluntariado (sem cargo/empresa/vínculo).
  await expect(
    volunteering.getByRole("textbox", { name: "Organização ou causa" }),
  ).toHaveValue("ONG Educar");
  await expect(
    volunteering.getByRole("textbox", { name: "Função" }),
  ).toHaveValue("Instrutor de programação");
  await expect(volunteering.getByText("Vínculo")).toHaveCount(0);
  await expect(
    volunteering.getByRole("switch", {
      name: "Atuo como voluntário atualmente",
    }),
  ).not.toBeChecked();

  // Adicionar voluntariado cria o cartão na seção certa.
  await volunteering
    .getByRole("button", { name: "Adicionar voluntariado" })
    .click();
  await expect(
    volunteering.getByRole("button", { name: /Nova atividade voluntária/ }),
  ).toBeVisible();
  await expect(
    professional.getByRole("button", { name: /Nova atividade voluntária/ }),
  ).toHaveCount(0);

  // "Voluntário" não é um tipo de vínculo empregatício.
  await professional.getByRole("button", { name: /Tech Lead/ }).click();
  await professional.getByRole("combobox").first().click();
  await expect(page.getByRole("option", { name: "CLT" })).toBeVisible();
  await expect(page.getByRole("option", { name: "Voluntário" })).toHaveCount(0);
});
