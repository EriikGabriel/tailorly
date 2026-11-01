import { Switch } from "@animate/base/switch";
import { GuidelineChips } from "@components/generation/guideline-chips";
import { LayoutPicker } from "@components/generation/layout-picker";
import { useProfileGenerationDefaults } from "@queries/profile-generation-defaults";

/**
 * Padrões de geração da conta: o que vale para toda vaga até você mudar. O gerador parte daqui e
 * deixa ajustar diretrizes e layout só para a vaga em andamento.
 */
export function AiGenerationDefaultsCard() {
  const { isAccount, isLoading, current, setDraft } =
    useProfileGenerationDefaults();
  const disabled = !isAccount || isLoading;
  const toggleGuideline = (label: string) =>
    setDraft({
      ...current,
      guidelines: current.guidelines.includes(label)
        ? current.guidelines.filter((item) => item !== label)
        : [...current.guidelines, label],
    });

  return (
    <div
      className={`flex flex-col gap-4 pt-2 ${disabled ? "pointer-events-none opacity-60" : ""}`}
    >
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-title-md font-semibold text-primary-950">
          Padrões de geração
        </h3>
        <span className="text-label-sm font-semibold text-on-surface-variant">
          Ponto de partida de cada vaga
        </span>
      </div>

      <ToggleRow
        title="Otimização Rigorosa para Robôs ATS (Workday, Greenhouse, Lever, Taleo)"
        description="Mede a nota do PDF e ajusta a ordem e a densidade do layout sem alterar fatos aprovados. Desligada, o currículo sai sem iterações de ajuste."
        checked={current.optimizeForAts}
        disabled={disabled}
        onCheckedChange={(optimizeForAts) =>
          setDraft({ ...current, optimizeForAts })
        }
      />

      <div className="flex flex-col gap-2">
        <p className="text-label-md font-semibold text-primary-950">
          Diretrizes de destaque padrão
        </p>
        <p className="text-body-sm text-on-surface-variant">
          A IA dá ênfase a esses pontos na reescrita e na ordem das seções, só
          com o que já está aprovado. No gerador você pode ajustar por vaga.
        </p>
        <GuidelineChips
          active={current.guidelines}
          onToggle={toggleGuideline}
        />
      </div>

      <LayoutPicker
        label="Layout padrão"
        value={current.templateName}
        onChange={(templateName) => setDraft({ ...current, templateName })}
      />
    </div>
  );
}

function ToggleRow({
  title,
  description,
  checked,
  disabled = false,
  onCheckedChange,
}: {
  title: string;
  description: string;
  checked: boolean;
  disabled?: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg bg-surface-container-low ">
      <div className="flex flex-col gap-0.5">
        <p className="text-label-md font-semibold text-primary-950">{title}</p>
        <p className="text-body-sm text-on-surface-variant">{description}</p>
      </div>
      <Switch
        checked={checked}
        disabled={disabled}
        onCheckedChange={onCheckedChange}
        aria-label={title}
      />
    </div>
  );
}
