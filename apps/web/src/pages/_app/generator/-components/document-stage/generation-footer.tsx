import type { CvGenerationResponse } from "@lib/api/generated/model";
import { Sparkles } from "@react-zero-ui/icon-sprite";
import { AtsScoreCard } from "../ats-score-card";

/** Nota de ATS e o lembrete de que a IA só ajusta a apresentação, nunca escreve fatos. */
export function GenerationFooter({
  generation,
}: {
  generation: CvGenerationResponse;
}) {
  return (
    <div className="w-full">
      <AtsScoreCard generation={generation} />
      <div className="mt-4 flex w-full flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-3 py-2 text-xs text-on-surface-variant">
        <span>Gerado apenas a partir do seu Currículo Base aprovado.</span>
        <span className="flex items-center gap-1.5">
          <Sparkles className="size-3.25" aria-hidden="true" />A IA só ajusta a
          apresentação, nunca escreve fatos.
        </span>
      </div>
    </div>
  );
}
