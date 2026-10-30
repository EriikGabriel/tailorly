/** Textos do diálogo de validação rápida, que dependem só da quantidade e do modo de priorização. */

const plural = (count: number, one: string, many: string) =>
  `${count} ${count === 1 ? one : many}`;

export function quickReviewHeading(count: number, prioritized: boolean) {
  if (count === 0) return "Tudo pronto para gerar";
  return prioritized
    ? `Confirme ${plural(count, "informação importante", "informações importantes")} para esta vaga`
    : `Confirme ${plural(count, "informação", "informações")} antes de gerar`;
}

export function quickReviewDescription(prioritized: boolean) {
  return prioritized
    ? "Escolhemos os fatos do seu currículo que sustentam o que a vaga mais pede. As respostas são registradas na versão do Currículo Base."
    : "Pendências persistidas na versão candidata do Currículo Base. As respostas são registradas na API.";
}

export function quickReviewListLabel(count: number, prioritized: boolean) {
  if (count === 0) return "Nenhuma pendência";
  return prioritized
    ? "Itens relevantes para esta vaga"
    : "Itens que exigem sua confirmação";
}

export function quickReviewEmptyMessage(
  prioritized: boolean,
  hasBaseVersion: boolean,
) {
  if (prioritized) {
    return "Nenhuma pendência do seu currículo está ligada ao que a vaga pede.";
  }
  return hasBaseVersion
    ? "Não há pendências reais nesta versão candidata."
    : "Envie o Currículo Base para carregar pendências reais.";
}

export function otherPendingNotice(otherPending: number) {
  return `Há ${plural(otherPending, "outra pendência", "outras pendências")} sem relação com esta vaga. Elas não bloqueiam, mas fatos pendentes não entram no currículo: resolva-as na revisão completa.`;
}

export function fullReviewNotice(shown: number, total: number) {
  return `${
    total > shown
      ? `Mostrando ${shown} de ${total} pendências. `
      : "Este currículo exige revisão de mais detalhes. "
  }Continue na revisão completa antes de gerar.`;
}

export function unprioritizedNotice(analysisCompleted: boolean) {
  return analysisCompleted
    ? "A vaga não nomeia tecnologias ou competências que possamos procurar nos seus fatos; mostrando todas as pendências."
    : "Sem a análise por IA da vaga não dá para priorizar os fatos; mostrando todas as pendências.";
}
