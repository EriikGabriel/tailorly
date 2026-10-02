import { expect, test } from "@playwright/test";

test(// O título vira parte do relatório e liga este cenário ao requisito funcional.
"RF16: bloquear a revisão até receber uma correção válida e diferente do original", {
  // Permite executar somente os cenários que cobrem RF16: `pnpm test:e2e --grep @RF16`.
  tag: "@RF16",
  annotation: {
    // A annotation aparece no relatório HTML como contexto de produto, não como
    // detalhe de implementação do teste.
    type: "requirement",
    description: "RF16 — revisão rápida: gate de completude na prévia local",
  },
}, async ({ page }) => {
  // Prepara as duas entradas mínimas para habilitar a ação que abre a revisão.
  // O PDF é uma fixture sintética: o teste não usa currículo de uma pessoa.
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
    name: "Prévia da revisão de currículo",
  });
  const continueButton = dialog.getByRole("button", {
    name: "Continuar para prévia",
  });

  // Confirma que a interface deixa claro que as pendências são fixtures. Em
  // seguida, prova que não basta abrir o modal para seguir adiante.
  await expect(dialog).toContainText(
    "Demonstração com dados fictícios, não extraídos do seu arquivo.",
  );
  await expect(continueButton).toBeDisabled();

  // Resolve a pendência temporal e seleciona a alternativa que requer a
  // correção da métrica. Assim o campo numérico e sua validação ficam visíveis.
  await dialog.getByRole("button", { name: "Sim, ainda atuo" }).click();
  await dialog.getByRole("button", { name: "Corrigir valor" }).click();

  const correction = dialog.getByRole("spinbutton", {
    name: "Economia anual em US$",
  });

  // Campo vazio ainda deixa a pendência incompleta.
  await expect(correction).toBeEmpty();
  await expect(continueButton).toBeDisabled();

  // O valor original não é uma correção. A regra exige uma mudança factual.
  await correction.fill("45000");
  await expect(correction).toHaveAttribute("aria-invalid", "true");
  await expect(continueButton).toBeDisabled();

  // O mínimo da métrica é 1; valores negativos também não completam a revisão.
  await correction.fill("-1");
  await expect(correction).toHaveAttribute("aria-invalid", "true");
  await expect(continueButton).toBeDisabled();

  // Um valor válido, permitido e diferente do original responde todas as
  // pendências da revisão rápida e libera a continuidade.
  await correction.fill("50000");
  await expect(correction).toHaveAttribute("aria-invalid", "false");
  await expect(continueButton).toBeEnabled();

  // Regressão: apagar uma resposta antes válida deve restaurar o bloqueio.
  await correction.fill("");
  await expect(continueButton).toBeDisabled();

  // Com a revisão novamente completa, a prévia navega para a tela seguinte.
  // Isso testa a consequência observável do gate, sem afirmar geração real.
  await correction.fill("50000");
  await continueButton.click();
  await expect(page).toHaveURL(/\/generator$/);
});
