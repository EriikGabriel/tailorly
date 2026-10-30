import { Clock, FileText } from "@react-zero-ui/icon-sprite";
import { AiGenerationDefaultsCard } from "./ai-generation-defaults-card";

export function AiGenerationPreviewCard() {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
          <FileText aria-hidden="true" className="size-4" />
          Geração de Currículo sob ATS
        </h3>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-card px-2.5 py-0.5 text-label-sm font-medium text-on-surface-variant">
          <Clock aria-hidden="true" className="size-3" />
          Planejado (RF03)
        </span>
      </div>
      <p className="text-body-sm text-on-surface-variant">
        Ainda não existe endpoint de geração (<code>POST /v1/cv/generate</code>)
        no backend. Quando implementado, deve reutilizar o mesmo par
        OpenAI/Ollama configurado na extração acima (RNF07). Os campos abaixo
        mostram como a configuração vai funcionar, mas estão desativados nesta
        prévia.
      </p>
      <div aria-disabled="true" className="pointer-events-none opacity-60">
        <AiGenerationDefaultsCard />
      </div>
    </div>
  );
}
