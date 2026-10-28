import { Switch } from "@animate/base/switch";
import { Check, Plus, Target } from "@react-zero-ui/icon-sprite";
import { useId, useState } from "react";
import { SectionHeading } from "./section-heading";

const availableGuidelines = [
  "Destacar Liderança Técnica",
  "Arquitetura em Nuvem & Escala",
  "Impacto de Negócio Quantificado",
  "Projetos Open Source",
] as const;

export function AiGuidelinesCard() {
  const atsSwitchId = useId();
  const [active, setActive] = useState<Set<string>>(
    () => new Set(availableGuidelines.slice(0, 3)),
  );
  const [optimizeForAts, setOptimizeForAts] = useState(true);

  function toggle(label: string) {
    setActive((current) => {
      const next = new Set(current);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });
  }

  return (
    <div className="flex flex-col gap-3 border-b border-outline-variant/60 pb-4">
      <SectionHeading
        step={3}
        title="Diretrizes de Destaque da IA"
        trailing={`${active.size} ${active.size === 1 ? "ativa" : "ativas"}`}
      />

      <p className="text-[13px] leading-5 text-on-surface-variant">
        A IA prioriza essas diretrizes aplicando-as rigorosamente aos dados do
        anúncio e do seu Currículo Base oficial.
      </p>

      <fieldset className="flex flex-wrap gap-1.5 border-0 p-0 m-0">
        <legend className="sr-only">Diretrizes de destaque</legend>
        {availableGuidelines.map((label) => {
          const isActive = active.has(label);
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
              Sintaxe pura, sem colunas complexas, densidade máxima de
              palavras-chave do anúncio.
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
    </div>
  );
}
