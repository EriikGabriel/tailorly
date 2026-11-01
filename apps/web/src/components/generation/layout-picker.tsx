import { templateOptions } from "@/constants/generation-options";

/** Escolha do layout do currículo; usado no perfil (padrão) e no gerador (por vaga). */
export function LayoutPicker({
  value,
  onChange,
  label = "Layout do currículo",
}: {
  value: string;
  onChange: (value: string) => void;
  label?: string;
}) {
  return (
    <div className="flex flex-col gap-2 border-0 p-0 m-0">
      <legend className="text-sm font-semibold tracking-[0.14px] text-on-surface">
        {label}
      </legend>
      <div className="grid gap-2 sm:grid-cols-2">
        {templateOptions.map((option) => {
          const isActive = value === option.value;
          return (
            <button
              key={option.value}
              type="button"
              aria-pressed={isActive}
              onClick={() => onChange(option.value)}
              className={`flex flex-col items-start gap-1 rounded-lg p-3 text-left shadow-xs transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring border ${
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
    </div>
  );
}
