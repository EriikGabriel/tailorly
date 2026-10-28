import { Toggle, ToggleGroup } from "@animate/base/toggle-group";
import { PROFICIENCY_LEVELS, proficiencyIndex } from "@/lib/review-format";

interface ProficiencyFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
}

function Dots({ filled }: { filled: number }) {
  return (
    <span aria-hidden="true" className="flex gap-0.5">
      {PROFICIENCY_LEVELS.map((level, index) => (
        <span
          key={level.value}
          className={`size-1.5 rounded-full ${index < filled ? "bg-current" : "bg-current opacity-25"}`}
        />
      ))}
    </span>
  );
}

/** Nível de proficiência em 5 degraus; um texto não reconhecido é mostrado e pode ser trocado. */
export function ProficiencyField({
  id,
  label,
  value,
  onChange,
}: ProficiencyFieldProps) {
  const index = proficiencyIndex(value);
  const selected = index >= 0 ? PROFICIENCY_LEVELS[index].value : "";
  const unrecognized = value.trim() !== "" && index < 0;

  return (
    <div className="space-y-1.5">
      <ToggleGroup
        id={id}
        aria-label={label}
        value={selected ? [selected] : []}
        onValueChange={(next: string[]) => {
          if (next[0]) onChange(next[0]);
        }}
        className="grid w-full grid-cols-5 gap-1 rounded-lg border border-outline-variant/60 bg-surface-container-low p-1"
      >
        {PROFICIENCY_LEVELS.map((level, levelIndex) => (
          <Toggle
            key={level.value}
            value={level.value}
            aria-label={level.label}
            className="h-auto min-h-10 w-full flex-col gap-1 px-1 py-1.5 text-[11px] font-semibold text-on-surface-variant transition-colors data-pressed:bg-primary-900 data-pressed:text-primary-foreground"
          >
            <Dots filled={levelIndex + 1} />
            {level.label}
          </Toggle>
        ))}
      </ToggleGroup>
      {unrecognized && (
        <p className="text-xs text-on-surface-variant">
          Valor do documento:{" "}
          <span className="font-semibold text-primary-950">{value}</span>.
          Escolha um nível para padronizar.
        </p>
      )}
    </div>
  );
}
