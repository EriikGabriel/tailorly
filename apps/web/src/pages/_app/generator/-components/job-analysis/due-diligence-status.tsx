import { useDueDiligenceHistory } from "@queries/due-diligence";
import { Link } from "@tanstack/react-router";

/**
 * Situação da due diligence da empresa da vaga, etapa da esteira que roda sozinha depois do Ground Truth.
 * Só informa e leva ao relatório; nada daqui entra no currículo.
 */
export function DueDiligenceStatus({ postingId }: { postingId: string }) {
  const history = useDueDiligenceHistory();
  const entry = history.data?.find((item) => item.jobPostingId === postingId);
  if (!entry) return null;

  const label =
    entry.status === "running"
      ? "Due diligence da empresa em andamento..."
      : entry.status === "failed"
        ? "Due diligence automática não concluída."
        : `Due diligence pronta: ${entry.factorsWithEvidence}/${entry.factorsTotal} fatores com fontes.`;

  return (
    <p className="flex flex-wrap items-center gap-x-2 text-xs text-on-surface-variant">
      <span role="status">{label}</span>
      <Link
        to="/due-diligence"
        className="font-semibold text-secondary-700 hover:underline"
      >
        Abrir
      </Link>
    </p>
  );
}
