import { CircleCheck, FileText } from "@react-zero-ui/icon-sprite";
import { AiGenerationDefaultsCard } from "./ai-generation-defaults-card";

export function AiGenerationPreviewCard() {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
          <FileText aria-hidden="true" className="size-4" />
          Geração de Currículo sob ATS
        </h3>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-on-tertiary-container/20 border border-on-tertiary-container px-2.5 py-0.5 text-label-sm font-medium text-on-tertiary-container">
          <CircleCheck aria-hidden="true" className="size-3" />
          Ativo
        </span>
      </div>
      <p className="text-body-sm text-on-surface-variant">
        Essas diretrizes são aplicadas imediatamente ao próximo currículo
        gerado. A otimização mede a nota ATS do PDF no servidor; a IA só
        reorganiza fatos já aprovados no Currículo Base.
      </p>
      <AiGenerationDefaultsCard />
    </div>
  );
}
