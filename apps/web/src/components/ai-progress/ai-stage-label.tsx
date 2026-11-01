import { ThinkingOrb } from "thinking-orbs";
import { cn } from "@/lib/utils";

/** Animações do `thinking-orbs` que usamos, cada uma com o sentido da etapa. */
export type OrbState =
  | "working"
  | "searching"
  | "solving"
  | "connecting"
  | "weaving"
  | "composing"
  | "breathing"
  | "shaping";

/**
 * Indicador compacto de etapa da IA, feito para ficar dentro de um botão ou de uma linha de status:
 * uma bolinha animada e o rótulo da etapa atual.
 */
export function AiStageLabel({
  orb,
  label,
  theme = "dark",
  title,
  className,
}: {
  orb: OrbState;
  label: string;
  theme?: "dark" | "light";
  title?: string;
  className?: string;
}) {
  return (
    <span
      aria-live="polite"
      title={title}
      className={cn("inline-flex min-w-0 items-center gap-2", className)}
    >
      <ThinkingOrb
        theme={theme}
        color="#ffffff"
        state={orb}
        size={20}
        aria-label={label}
      />
      <span className="truncate">{label}</span>
    </span>
  );
}
