import { reviewSteps } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import {
  BadgeAlert,
  BadgeCheck,
  ShieldCheck,
} from "@react-zero-ui/icon-sprite";
import type { ReactNode } from "react";
import { useState } from "react";
import { usePendingFields } from "../-hooks/use-pending-fields";

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
  const pending = usePendingFields();
  const stepId = reviewSteps[number - 1].id;
  const clear = pending.stepClear(stepId);
  const stepIssues = pending.stepIssues(stepId);
  const [confirming, setConfirming] = useState(false);

  async function confirmAll() {
    setConfirming(true);
    for (const issue of stepIssues) {
      await pending.confirm(issue);
    }
    setConfirming(false);
  }

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
        <div className="flex shrink-0 items-center gap-2">
          {!clear && stepIssues.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              disabled={confirming || pending.busy}
              onClick={confirmAll}
              className="h-auto gap-1 px-2 py-1 text-xs text-tertiary-700 hover:bg-tertiary-50 hover:text-tertiary-700"
            >
              <BadgeAlert aria-hidden="true" className="size-3.5" />
              Validar todos ({stepIssues.length})
            </Button>
          )}
          {clear ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-tertiary-100 px-3 py-1 text-xs font-semibold text-tertiary-700">
              <BadgeCheck aria-hidden="true" className="size-4" />
              Validado
            </span>
          ) : (
            <span className="rounded-full bg-surface-container px-3 py-1 text-xs font-semibold text-on-surface-variant">
              {editorial ? "Ajustes editoriais" : "Aguardando revisão"}
            </span>
          )}
        </div>
      </header>
      <p
        className={`flex items-start gap-1.5 rounded-lg border-l-4 bg-surface-container-low py-1.5 pr-2 pl-3 text-xs leading-5 text-on-surface-variant ${number === 3 ? "border-on-tertiary-container" : "border-primary-950"}`}
      >
        <ShieldCheck
          aria-hidden="true"
          className="mt-0.5 size-3.5 shrink-0 text-primary-950"
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
