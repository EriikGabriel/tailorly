import { Check, Plus } from "@react-zero-ui/icon-sprite";
import { availableGuidelines } from "@stores/generation-preferences-store";

/** Diretrizes de destaque como chips ligáveis; usado no perfil (padrão) e no gerador (por vaga). */
export function GuidelineChips({
  active,
  onToggle,
}: {
  active: readonly string[];
  onToggle: (label: string) => void;
}) {
  return (
    <fieldset className="flex flex-wrap gap-1.5 border-0 p-0 m-0">
      <legend className="sr-only">Diretrizes de destaque</legend>
      {availableGuidelines.map((label) => {
        const isActive = active.includes(label);
        return (
          <button
            key={label}
            type="button"
            aria-pressed={isActive}
            onClick={() => onToggle(label)}
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
  );
}
