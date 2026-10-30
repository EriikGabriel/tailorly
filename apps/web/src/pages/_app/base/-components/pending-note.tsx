import { BadgeAlert } from "@react-zero-ui/icon-sprite";
import type { ReviewIssue } from "@/types/review";
import { usePendingFields } from "../-hooks/use-pending-fields";

/**
 * Aviso no próprio campo: por que ele está pendente, o quanto importa para a vaga e como resolver
 * (editar e salvar, limpar e salvar, ou confirmar o valor atual).
 */
export function PendingNote({
  issue,
  inline = false,
}: {
  issue: ReviewIssue;
  inline?: boolean;
}) {
  const pending = usePendingFields();
  const edited = pending.edited(issue);
  const relevance = issue.jobRelevance;

  return (
    <div
      className={`flex flex-wrap items-center gap-1.5 text-[11px] ${inline ? "" : "mt-1"}`}
    >
      <span className="inline-flex items-center gap-1 rounded-full bg-tertiary-100 px-2 py-0.5 font-bold text-tertiary-900">
        <BadgeAlert aria-hidden="true" className="size-3.5" />
        Pendente
      </span>
      {relevance && (
        <span
          className={`rounded-full px-2 py-0.5 font-bold ${relevance.importance === "required" ? "bg-primary-900 text-primary-foreground" : "bg-surface-container text-on-surface"}`}
          title={relevance.label}
        >
          {relevance.importance === "required"
            ? "Obrigatório na vaga"
            : "Desejável na vaga"}
          {" · "}
          {relevance.label}
        </span>
      )}
      {edited ? (
        <span className="text-on-surface-variant">
          Salve a auditoria para registrar a alteração.
        </span>
      ) : (
        <button
          type="button"
          disabled={pending.busy}
          onClick={() => pending.confirm(issue)}
          className="rounded-md border border-outline-variant/60 bg-card px-2 py-0.5 font-semibold text-primary-950 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900 disabled:opacity-50"
        >
          Confirmar valor
        </button>
      )}
    </div>
  );
}
