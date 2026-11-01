import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { useProfileGenerationDefaults } from "@queries/profile-generation-defaults";
import { useReducedMotion } from "motion/react";

type Step = {
  name: string;
  detail: string;
  active: boolean;
  statusLabel: string;
};

/**
 * Etapas do fluxo que usam o provedor de IA escolhido acima. Vem recolhida para não ocupar espaço;
 * o status reflete o que já está implementado no backend, e a due diligence ainda é backlog.
 */
export function AiStepsToggle({
  open,
  onToggle,
  controlsId,
}: {
  open: boolean;
  onToggle: () => void;
  controlsId: string;
}) {
  return (
    <button
      type="button"
      aria-expanded={open}
      aria-controls={controlsId}
      onClick={onToggle}
      className="text-label-sm font-semibold text-primary-950 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
    >
      {open
        ? "Ocultar etapas que usam o provedor"
        : "Ver etapas que usam o provedor"}
    </button>
  );
}

export function AiStepsList({ open, id }: { open: boolean; id: string }) {
  const { current } = useProfileGenerationDefaults();
  const reduceMotion = useReducedMotion();

  const steps: Step[] = [
    {
      name: "Extração do Currículo Base (Ground Truth)",
      detail: "Interpreta o texto do CV em fatos, com evidência de origem.",
      active: true,
      statusLabel: "Ativo",
    },
    {
      name: "Análise da vaga",
      detail: "Extrai requisitos e palavras-chave da descrição da vaga.",
      active: true,
      statusLabel: "Ativo",
    },
    {
      name: "Esteira de ATS (layout e revisão)",
      detail:
        "Ajusta a ordem e a densidade do layout e revisa a leitura do PDF.",
      active: current.optimizeForAts,
      statusLabel: current.optimizeForAts
        ? "Ativo"
        : "Desligado nos padrões de ATS",
    },
    {
      name: "Enriquecimento e evidências",
      detail:
        "Reescreve experiências com a vaga e busca detalhes em links e no vault.",
      active: true,
      statusLabel: "Ativo",
    },
    {
      name: "Due diligence de empresas",
      detail: "Sinais públicos sobre empresas antes da candidatura.",
      active: false,
      statusLabel: "Backlog",
    },
  ];

  return (
    <AutoHeight
      deps={[open]}
      transition={reduceMotion ? { duration: 0 } : undefined}
    >
      {open && (
        <ul id={id} className="flex flex-col gap-2">
          {steps.map((step) => (
            <li
              key={step.name}
              className="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low p-3"
            >
              <div className="flex min-w-0 flex-col gap-0.5">
                <p className="text-label-md font-semibold text-primary-950">
                  {step.name}
                </p>
                <p className="text-label-sm text-on-surface-variant">
                  {step.detail}
                </p>
              </div>
              <span
                className={
                  step.active
                    ? "shrink-0 text-label-sm font-semibold text-on-secondary-container"
                    : "shrink-0 text-label-sm font-semibold text-on-surface-variant"
                }
              >
                {step.statusLabel}
              </span>
            </li>
          ))}
        </ul>
      )}
    </AutoHeight>
  );
}
