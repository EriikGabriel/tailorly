import { Switch } from "@animate/base/switch";
import { useGenerationPreferencesStore } from "@stores/generation-preferences-store";

export function AiGenerationDefaultsCard() {
  const optimizeForAts = useGenerationPreferencesStore(
    (state) => state.optimizeForAts,
  );
  const guidelines = useGenerationPreferencesStore((state) => state.guidelines);
  const setOptimizeForAts = useGenerationPreferencesStore(
    (state) => state.setOptimizeForAts,
  );
  const toggleGuideline = useGenerationPreferencesStore(
    (state) => state.toggleGuideline,
  );
  const quantifiedResults = guidelines.includes(
    "Impacto de Negócio Quantificado",
  );

  return (
    <div className="flex flex-col gap-3 pt-2">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-title-md font-semibold text-primary-950">
          Diretrizes Padrão de Formatação ATS
        </h3>
        <span className="text-label-sm font-semibold text-on-surface-variant">
          Aplicadas na próxima geração
        </span>
      </div>

      <div className="flex flex-col gap-2">
        <ToggleRow
          title="Otimização Rigorosa para Robôs ATS (Workday, Greenhouse, Lever, Taleo)"
          description="Mede a nota do PDF e ajusta a ordem e a densidade do layout sem alterar fatos aprovados."
          checked={optimizeForAts}
          onCheckedChange={setOptimizeForAts}
        />
        <ToggleRow
          title="Priorizar Resultados Quantificados"
          description="Pede à IA para destacar, por ordem de apresentação, os fatos aprovados que já têm impacto mensurável."
          checked={quantifiedResults}
          onCheckedChange={() =>
            toggleGuideline("Impacto de Negócio Quantificado")
          }
        />
      </div>
    </div>
  );
}

function ToggleRow({
  title,
  description,
  checked,
  onCheckedChange,
}: {
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg bg-surface-container-low p-2">
      <div className="flex flex-col gap-0.5">
        <p className="text-label-md font-semibold text-primary-950">{title}</p>
        <p className="text-body-sm text-on-surface-variant">{description}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}
