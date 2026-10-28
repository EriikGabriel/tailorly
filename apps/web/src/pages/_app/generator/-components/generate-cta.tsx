import { Button } from "@animate/buttons/button";
import { ArrowRight, Sparkles } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useQuickReviewStore } from "@stores/quick-review-store";

export function GenerateCta() {
  const baseCv = useBaseCvStore((state) => state.file);
  const { mode, jobUrl, jobText, saveDraft } = useJobDraftStore();
  const setReviewOpen = useQuickReviewStore((state) => state.setOpen);
  const hasJob =
    mode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;
  const canGenerate = hasJob && Boolean(baseCv);

  function handleGenerate() {
    if (!canGenerate) return;
    saveDraft();
    setReviewOpen(true);
  }

  return (
    <div className="flex flex-col gap-2 pt-1">
      <Button
        type="button"
        onClick={handleGenerate}
        disabled={!canGenerate}
        hoverScale={1.01}
        className="h-auto min-h-13 w-full gap-2 rounded-xl bg-primary-900 py-3.5 text-sm font-semibold tracking-[0.14px] text-primary-foreground shadow-md hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Sparkles aria-hidden="true" className="size-5 shrink-0" />
        Gerar Currículo Sob Medida com IA
        <ArrowRight aria-hidden="true" className="size-3 shrink-0" />
      </Button>
      <p className="px-1 text-xs text-on-surface-variant">
        {!baseCv
          ? "Configure seu Currículo Base para liberar a geração."
          : !hasJob
            ? "Informe a vaga alvo para liberar a geração."
            : "Vamos confirmar as pendências do Ground Truth antes de gerar."}
      </p>
      <div className="flex items-center justify-between px-1 py-1 text-xs font-semibold tracking-[0.3px] text-on-surface-variant">
        <span className="flex items-center gap-1.5">
          <span
            aria-hidden="true"
            className="size-1.5 rounded-full bg-on-tertiary-container"
          />
          Motor: GPT-4o + Ground Truth Validator
        </span>
      </div>
    </div>
  );
}
