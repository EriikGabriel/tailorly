import { ShieldCheck } from "@react-zero-ui/icon-sprite";
import type { ReactNode } from "react";

export function ReviewStepShell({
  number,
  title,
  description,
  rule,
  editorial = false,
  children,
}: {
  number: number;
  title: string;
  description: string;
  rule: string;
  editorial?: boolean;
  children: ReactNode;
}) {
  return (
    <section
      aria-labelledby={`review-heading-${number}`}
      className="flex min-w-0 flex-col gap-4 rounded-2xl bg-card p-4 shadow-xs sm:p-6"
    >
      <header className="flex flex-wrap items-start justify-between gap-3 border-b border-surface-container-high pb-2">
        <div className="flex min-w-0 items-start gap-2">
          <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-primary text-xs font-bold text-primary-foreground">
            {number}
          </span>
          <div className="min-w-0">
            <h2 id={`review-heading-${number}`} className="text-headline-sm">
              {title}
            </h2>
            <p className="text-body-sm">{description}</p>
          </div>
        </div>
        <span className="rounded-full bg-surface-container px-3 py-1 text-xs font-semibold text-on-surface-variant">
          {editorial ? "Ajustes editoriais" : "Aguardando revisão"}
        </span>
      </header>
      <p
        className={`flex items-start gap-1 rounded-xl border-l-4 bg-surface-container-low py-2 pr-2 pl-3 text-body-sm ${number === 3 ? "border-on-tertiary-container" : "border-primary-950"}`}
      >
        <ShieldCheck
          aria-hidden="true"
          className="mt-0.5 size-4 shrink-0 text-primary-950"
        />
        <span>
          <strong className="font-bold text-primary-950">
            {editorial ? "Nota de curadoria: " : "Regra de integridade: "}
          </strong>
          {rule}
        </span>
      </p>
      {children}
    </section>
  );
}
