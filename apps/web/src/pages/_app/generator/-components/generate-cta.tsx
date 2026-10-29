import { Button } from "@animate/buttons/button";
import { useJobTarget } from "@hooks/use-job-target";
import { ArrowRight, Sparkles } from "@react-zero-ui/icon-sprite";

export function GenerateCta() {
  const target = useJobTarget();

  return (
    <div className="flex flex-col gap-2 pt-1">
      <Button
        type="button"
        onClick={target.generate}
        disabled={!target.canGenerate || target.isAnalyzing}
        hoverScale={1.01}
        className="h-auto min-h-13 w-full gap-2 rounded-xl bg-primary-900 py-3.5 text-sm font-semibold tracking-[0.14px] text-primary-foreground shadow-md hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Sparkles aria-hidden="true" className="size-5 shrink-0" />
        {target.isAnalyzing
          ? "Lendo e analisando a vaga..."
          : "Gerar Currículo Sob Medida com IA"}
        <ArrowRight aria-hidden="true" className="size-3 shrink-0" />
      </Button>
      <p className="px-1 text-xs text-on-surface-variant">
        {target.error
          ? target.error
          : (target.blockReason ??
            "Vamos ler a vaga e confirmar os fatos que ela mais pede antes de gerar.")}
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
