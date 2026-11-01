/** Meta da esteira no servidor (`AtsEvaluator.TARGET_SCORE`): a partir daqui a nota é considerada boa. */
export const ATS_TARGET_SCORE = 90;
/** A partir daqui a nota ainda é aceitável, mesmo abaixo da meta. */
export const ATS_ACCEPTABLE_SCORE = 70;
/** Abaixo disto a cor já é o vermelho pleno. */
const ATS_FLOOR_SCORE = 40;

/** Cor contínua do vermelho (nota baixa) ao verde (meta atingida), legível sobre fundo claro. */
export function atsScoreColor(score: number): string {
  const ratio = Math.min(
    1,
    Math.max(
      0,
      (score - ATS_FLOOR_SCORE) / (ATS_TARGET_SCORE - ATS_FLOOR_SCORE),
    ),
  );
  return `hsl(${Math.round(ratio * 130)} 72% 34%)`;
}

export function atsScoreLabel(score: number): string {
  if (score >= ATS_TARGET_SCORE) return "Ótima";
  if (score >= ATS_ACCEPTABLE_SCORE) return "Aceitável";
  if (score >= 50) return "Abaixo do ideal";
  return "Baixa";
}
