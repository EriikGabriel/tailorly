import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { Cpu, ShieldAlert, Sparkles } from "@react-zero-ui/icon-sprite";
import { useReducedMotion } from "motion/react";
import { useState } from "react";
import {
  type InterpretationProvider,
  previewInterpretationSettings,
} from "@/data/profile-preview";
import { AiConnectionParametersCard } from "./ai-connection-parameters-card";
import { AiDueDiligencePreviewCard } from "./ai-due-diligence-preview-card";
import { AiFallbackCard } from "./ai-fallback-card";
import { AiGenerationPreviewCard } from "./ai-generation-preview-card";
import { AiProviderSelector } from "./ai-provider-selector";
import { SectionCard } from "./section-card";

export function AiPreferencesSection() {
  const [provider, setProvider] = useState<InterpretationProvider>(
    previewInterpretationSettings.provider,
  );
  const reduceMotion = useReducedMotion();

  const needsRemote =
    provider === "remote" || provider === "remote-with-ollama-fallback";
  const needsOllama =
    provider === "remote-with-ollama-fallback" || provider === "ollama-only";

  return (
    <SectionCard
      icon={Cpu}
      title="4. Preferências de IA & Provedores de LLM"
      description="Configure os provedores de IA usados em cada etapa que depende de interpretação de linguagem natural: extração do Currículo Base, geração sob ATS e due diligence de empresas."
      trailing={
        <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary-container px-3 py-1 text-label-sm font-semibold text-on-secondary-container">
          <ShieldAlert aria-hidden="true" className="size-3.5" />
          Só a extração está ativa hoje
        </span>
      }
    >
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between gap-2">
          <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
            <Sparkles aria-hidden="true" className="size-4" />
            Extração do Currículo Base (Ground Truth) — Ativo
          </h3>
          <span className="font-mono text-label-sm font-semibold text-on-surface-variant">
            tailorly.interpretation.provider
          </span>
        </div>
        <p className="text-body-sm text-on-surface-variant">
          Define como os fatos são interpretados a partir do texto extraído do
          seu CV. É a única etapa com IA de fato implementada no backend hoje.
        </p>
        <AiProviderSelector value={provider} onChange={setProvider} />
      </div>

      <AutoHeight
        deps={[provider]}
        transition={reduceMotion ? { duration: 0 } : undefined}
      >
        <div className="flex flex-col gap-4 pb-px">
          {needsRemote && <AiConnectionParametersCard />}
          {needsOllama && (
            <AiFallbackCard
              mode={provider === "ollama-only" ? "primary" : "fallback"}
            />
          )}
          {provider === "rules" && (
            <p className="rounded-xl border border-outline-variant bg-surface-container-low p-4 text-body-sm text-on-surface-variant">
              Modo sem IA: a extração usa apenas regras determinísticas. Nenhuma
              credencial é necessária e o resultado está sempre disponível,
              mesmo offline ou sem provedores externos configurados.
            </p>
          )}
        </div>
      </AutoHeight>

      <AiGenerationPreviewCard />
      <AiDueDiligencePreviewCard />
    </SectionCard>
  );
}
