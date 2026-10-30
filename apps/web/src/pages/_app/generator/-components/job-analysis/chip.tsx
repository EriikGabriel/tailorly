import type { ReactNode } from "react";

const TONES = {
  neutral: "bg-surface-container text-on-surface-variant",
  primary: "bg-secondary-container text-on-secondary-container",
  warning: "bg-error-container text-on-error-container",
} as const;

/** Etiqueta arredondada para atributos da vaga (modelo de trabalho, tecnologia, prazo etc.). */
export function Chip({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: keyof typeof TONES;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold tracking-[0.3px] ${TONES[tone]}`}
    >
      {children}
    </span>
  );
}
