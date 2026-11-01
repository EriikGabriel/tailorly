import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { Cpu, RefreshCw, Sparkles } from "@react-zero-ui/icon-sprite";
import { useProfileSettingsStore } from "@stores/profile-settings-store";
import { useReducedMotion } from "motion/react";
import { useEffect, useId, useState } from "react";
import { useAiPreferences } from "@/queries/ai-preferences";
import { AiConnectionParametersCard } from "./ai-connection-parameters-card";
import { AiDueDiligencePreviewCard } from "./ai-due-diligence-preview-card";
import { AiFallbackCard } from "./ai-fallback-card";
import { AiGenerationPreviewCard } from "./ai-generation-preview-card";
import { AiProviderSelector } from "./ai-provider-selector";
import { AiStepsList, AiStepsToggle } from "./ai-steps-list";
import { SectionCard } from "./section-card";

export function AiPreferencesSection() {
  const provider = useProfileSettingsStore((state) => state.aiDraft.provider);
  const updateProvider = useProfileSettingsStore(
    (state) => state.updateProvider,
  );
  const aiLoaded = useProfileSettingsStore((state) => state.aiLoaded);
  const hydrateAiPreferences = useProfileSettingsStore(
    (state) => state.hydrateAiPreferences,
  );
  const reduceMotion = useReducedMotion();
  const [stepsOpen, setStepsOpen] = useState(false);
  const stepsId = useId();

  const { data: aiPreferences } = useAiPreferences();

  useEffect(() => {
    if (aiPreferences) hydrateAiPreferences(aiPreferences);
  }, [aiPreferences, hydrateAiPreferences]);

  const needsRemote =
    provider === "remote" || provider === "remote-with-ollama-fallback";
  const needsOllama =
    provider === "remote-with-ollama-fallback" || provider === "ollama-only";

  return (
    <SectionCard
      icon={Cpu}
      title="4. Preferências de IA & Provedores de LLM"
      description="Configure os provedores de IA usados em cada etapa que depende de interpretação de linguagem natural: extração do Currículo Base, geração sob ATS e due diligence de empresas."
    >
      {!aiLoaded ? (
        <p className="flex items-center gap-2 rounded-xl border border-outline-variant bg-surface-container-low p-4 text-body-sm text-on-surface-variant">
          <RefreshCw aria-hidden="true" className="size-3.5 animate-spin" />
          Carregando preferências de IA da sua conta...
        </p>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between gap-2">
              <div className="flex flex-wrap items-center gap-5">
                <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
                  <Sparkles aria-hidden="true" className="size-4" />
                  Provedor de IA
                </h3>
              </div>
              <AiStepsToggle
                open={stepsOpen}
                onToggle={() => setStepsOpen((value) => !value)}
                controlsId={stepsId}
              />
            </div>
            <p className="text-body-sm text-on-surface-variant">
              Escolha como as etapas com IA são executadas. O mesmo provedor
              atende todas as etapas listadas abaixo.
            </p>
            <AiProviderSelector value={provider} onChange={updateProvider} />
            <AiStepsList open={stepsOpen} id={stepsId} />
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
                  Modo sem IA: as etapas usam apenas regras determinísticas.
                  Nenhuma credencial é necessária e o resultado está sempre
                  disponível, mesmo offline ou sem provedores externos
                  configurados.
                </p>
              )}
            </div>
          </AutoHeight>
        </div>
      )}

      <AiGenerationPreviewCard />
      <AiDueDiligencePreviewCard />
    </SectionCard>
  );
}
