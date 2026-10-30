import { Button } from "@components/ui/animate/buttons/button";
import { ArrowRight, CheckCheck, Sparkles } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

type DialogFooterProps = {
  needsFullReview: boolean;
  hasQuickIssues: boolean;
  canConfirmAll: boolean;
  canFinish: boolean;
  generating: boolean;
  onClose: () => void;
  onConfirmAll: () => void;
  onFinish: () => void;
  onGoToFullReview: () => void;
};

/** Rodapé do diálogo: dispensar, confirmar tudo e gerar o currículo (ou seguir à revisão completa). */
export function DialogFooter({
  needsFullReview,
  hasQuickIssues,
  canConfirmAll,
  canFinish,
  generating,
  onClose,
  onConfirmAll,
  onFinish,
  onGoToFullReview,
}: DialogFooterProps) {
  return (
    <div className="flex shrink-0 flex-col  bg-surface-container-low border-t border-outline-variant/30 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7">
      <Link
        to="/base"
        onClick={onGoToFullReview}
        className="text-xs font-semibold text-secondary-700 underline underline-offset-2 hover:text-primary-900"
      >
        Revisar currículo completo
      </Link>
      <div className="flex flex-wrap gap-6">
        <Button
          variant="outline"
          type="button"
          onClick={onClose}
          className="rounded-xl border border-outline-variant/50 p-6 text-sm font-medium hover:bg-surface-container"
          disableZoom
        >
          Agora não
        </Button>
        {!needsFullReview && hasQuickIssues && (
          <Button
            variant="outline"
            type="button"
            onClick={onConfirmAll}
            disabled={!canConfirmAll}
            className="rounded-xl border border-tertiary-200 bg-tertiary-50 p-6 text-sm font-medium text-tertiary-900 hover:bg-tertiary-100 disabled:cursor-not-allowed disabled:opacity-40"
            disableZoom
          >
            <CheckCheck
              strokeWidth={1.5}
              className="size-4 shrink-0"
              aria-hidden="true"
            />
            Confirmar todos
          </Button>
        )}
        {needsFullReview ? (
          <Link
            to="/base"
            onClick={onGoToFullReview}
            className="rounded-xl flex justify-center items-center bg-primary-900 p-4 text-sm font-semibold text-primary-foreground hover:bg-primary-800"
          >
            Ir para revisão completa
          </Link>
        ) : (
          <Button
            type="button"
            onClick={onFinish}
            disabled={!canFinish}
            className="rounded-xl bg-primary-900 p-6 text-sm font-semibold text-primary-foreground hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-40"
            disableZoom
          >
            <Sparkles
              strokeWidth={1.5}
              className="size-4 shrink-0"
              aria-hidden="true"
            />
            {generating ? "Gerando currículo..." : "Gerar currículo"}
            <ArrowRight
              strokeWidth={1.5}
              className="size-4 shrink-0"
              aria-hidden="true"
            />
          </Button>
        )}
      </div>
    </div>
  );
}
