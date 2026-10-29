import { Button } from "@animate/buttons/button";
import { ShieldCheck } from "@react-zero-ui/icon-sprite";

export function ReviewCompletionCard({
  onSave,
  onApprove,
  onReviewIssues,
  saving = false,
  approving = false,
  disabled = false,
  error,
  pendingCount = 0,
}: {
  onSave: () => void;
  onApprove: () => void;
  onReviewIssues: () => void;
  saving?: boolean;
  approving?: boolean;
  disabled?: boolean;
  error?: string | null;
  pendingCount?: number;
}) {
  return (
    <section
      aria-labelledby="review-completion-heading"
      className="space-y-4 rounded-2xl bg-card p-4 shadow-md sm:p-6"
    >
      <div className="flex items-center gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <ShieldCheck aria-hidden="true" className="size-6" />
        </span>
        <div>
          <h2 id="review-completion-heading" className="text-title-md">
            Auditoria factual aguardando revisão
          </h2>
          <p id="review-completion-description" className="text-body-sm">
            Salve os fatos revisados na versão candidata do Currículo Base. A
            aprovação final exige a confirmação das pendências.
          </p>
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button
          variant="accent"
          disabled={disabled || saving}
          onClick={onSave}
          aria-describedby="review-completion-description"
          className="h-auto min-h-12 whitespace-normal rounded-xl px-6 py-3.5"
        >
          {saving ? "Salvando..." : "Salvar Rascunho da Auditoria"}
        </Button>
        <Button
          disabled={disabled || approving || pendingCount > 0}
          onClick={onApprove}
          aria-describedby="review-completion-description"
          className="h-auto min-h-12 whitespace-normal rounded-xl px-6 py-3.5"
        >
          {approving ? "Aprovando..." : "Aprovar currículo base"}
        </Button>
      </div>
      {pendingCount > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-container-low p-3">
          <p className="text-body-sm">
            {pendingCount} pendência(s) obrigatória(s) ainda precisam de
            resposta.
          </p>
          <Button
            variant="ghost"
            onClick={onReviewIssues}
            className="rounded-lg"
          >
            Revisar pendências
          </Button>
        </div>
      )}
      {error && (
        <p role="alert" className="text-body-sm text-error">
          {error}
        </p>
      )}
    </section>
  );
}
