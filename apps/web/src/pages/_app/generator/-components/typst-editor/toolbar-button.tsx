import { Tooltip, TooltipPanel, TooltipTrigger } from "@animate/base/tooltip";
import type { ReactNode } from "react";

/** Botão de ícone da barra do editor, com dica (tooltip) e estado "ativo" para alternâncias. */
export function ToolbarButton({
  label,
  onClick,
  disabled,
  active,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  active?: boolean;
  children: ReactNode;
}) {
  return (
    <Tooltip>
      <TooltipTrigger
        type="button"
        onClick={onClick}
        disabled={disabled}
        aria-label={label}
        aria-pressed={active}
        className={`flex size-7 items-center justify-center rounded-md text-on-surface-variant outline-none transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-40 disabled:hover:bg-transparent ${
          active ? "bg-secondary-container text-on-secondary-container" : ""
        }`}
      >
        {children}
      </TooltipTrigger>
      <TooltipPanel>{label}</TooltipPanel>
    </Tooltip>
  );
}
