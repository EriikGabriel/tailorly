import { Switch } from "@animate/base/switch";
import { useViewer } from "@queries/cv-generation";
import {
  useGenerationDefaults,
  useSaveGenerationDefaults,
} from "@queries/generation-defaults";
import { Check, Plus, Target } from "@react-zero-ui/icon-sprite";
import {
  availableGuidelines,
  useGenerationPreferencesStore,
} from "@stores/generation-preferences-store";
import { useEffect, useId } from "react";
import { templateOptions } from "@/constants/generation-options";
import { SectionHeading } from "./section-heading";

export function AiGuidelinesCard() {
  const atsSwitchId = useId();
  const active = useGenerationPreferencesStore((state) => state.guidelines);
  const optimizeForAts = useGenerationPreferencesStore(
    (state) => state.optimizeForAts,
  );
  const toggle = useGenerationPreferencesStore(
    (state) => state.toggleGuideline,
  );
  const setOptimizeForAts = useGenerationPreferencesStore(
    (state) => state.setOptimizeForAts,
  );
  const templateName = useGenerationPreferencesStore(
    (state) => state.templateName,
  );
  const setTemplateName = useGenerationPreferencesStore(
    (state) => state.setTemplateName,
  );
  const viewer = useViewer();
  const accountOwnerId = viewer.kind === "account" ? viewer.ownerId : null;
  const defaults = useGenerationDefaults(accountOwnerId, viewer.token);
  const saveDefaults = useSaveGenerationDefaults(accountOwnerId, viewer.token);

  // Ao carregar a conta, o layout salvo vira a escolha atual. Cada troca depois é gravada na conta.
  const savedTemplate = defaults.data?.templateName ?? null;
  useEffect(() => {
    if (savedTemplate) setTemplateName(savedTemplate);
  }, [savedTemplate, setTemplateName]);

  function chooseTemplate(value: string) {
    setTemplateName(value);
    if (accountOwnerId) saveDefaults.mutate(value);
  }

  return (
    <div className="flex flex-col gap-3 border-b border-outline-variant/60 pb-4">
      <SectionHeading
        step={3}
        title="Diretrizes de Destaque da IA"
        trailing={`${active.length} ${active.length === 1 ? "ativa" : "ativas"}`}
      />

      <p className="text-[13px] leading-5 text-on-surface-variant">
        A IA usa essas diretrizes só para ordenar o que já está aprovado no seu
        Currículo Base; ela não escreve nem inventa informações.
      </p>

      <fieldset className="flex flex-wrap gap-1.5 border-0 p-0 m-0">
        <legend className="sr-only">Diretrizes de destaque</legend>
        {availableGuidelines.map((label) => {
          const isActive = active.includes(label);
          return (
            <button
              key={label}
              type="button"
              aria-pressed={isActive}
              onClick={() => toggle(label)}
              className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold tracking-[0.3px] shadow-xs transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring ${
                isActive
                  ? "bg-primary-900 text-primary-foreground hover:bg-primary-800"
                  : "bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
              }`}
            >
              {isActive ? (
                <Check className="size-3" aria-hidden="true" />
              ) : (
                <Plus className="size-3" aria-hidden="true" />
              )}
              {label}
            </button>
          );
        })}
      </fieldset>

      <div className="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low p-3 shadow-xs">
        <div className="flex items-start gap-2.5">
          <Target
            aria-hidden="true"
            className="mt-0.5 size-4.5 shrink-0 text-on-tertiary-container"
          />
          <div>
            <label
              htmlFor={atsSwitchId}
              className="text-sm font-semibold tracking-[0.14px] text-on-surface"
            >
              Otimizar rigorosamente para ATS
            </label>
            <p className="text-[13px] leading-5 text-on-surface-variant">
              A esteira mede a nota de ATS do PDF e ajusta ordem e densidade do
              layout até melhorar (uma coluna, texto real, sem tabelas).
            </p>
          </div>
        </div>
        <Switch
          id={atsSwitchId}
          checked={optimizeForAts}
          onCheckedChange={setOptimizeForAts}
          aria-label="Otimizar rigorosamente para ATS"
        />
      </div>

      <fieldset className="flex flex-col gap-2 border-0 p-0 m-0">
        <legend className="text-sm font-semibold tracking-[0.14px] text-on-surface">
          Layout do currículo
        </legend>
        <div className="grid gap-2 sm:grid-cols-3">
          {templateOptions.map((option) => {
            const isActive = templateName === option.value;
            return (
              <button
                key={option.value}
                type="button"
                aria-pressed={isActive}
                onClick={() => chooseTemplate(option.value)}
                className={`flex flex-col items-start gap-1 rounded-lg p-3 text-left shadow-xs transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring ${
                  isActive
                    ? "bg-primary-900 text-primary-foreground"
                    : "bg-surface-container-low text-on-surface hover:bg-surface-container"
                }`}
              >
                <span className="text-xs font-semibold">{option.label}</span>
                <span
                  className={`text-[12px] leading-4 ${
                    isActive
                      ? "text-primary-foreground/80"
                      : "text-on-surface-variant"
                  }`}
                >
                  {option.description}
                </span>
              </button>
            );
          })}
        </div>
      </fieldset>
    </div>
  );
}
