import { type ReviewStepId, reviewSteps as steps } from "@@types/review-model";
import {
  Highlight,
  HighlightItem,
} from "@animate/primitives/effects/highlight";
import { BadgeCheck } from "@react-zero-ui/icon-sprite";
import { cn } from "cn";
import { useReducedMotion } from "motion/react";
import type { StepPending } from "@/utils/review-step-map";

export function ReviewStepper({
  activeStep,
  onStepChange,
  pending,
  clear,
}: {
  activeStep: ReviewStepId;
  onStepChange: (step: ReviewStepId) => void;
  pending?: Record<ReviewStepId, StepPending>;
  /** Etapas com fatos e nenhuma pendência aberta. */
  clear?: Record<ReviewStepId, boolean>;
}) {
  const reducedMotion = useReducedMotion();

  return (
    <nav
      aria-label="Etapas de revisão do Currículo Base"
      className="@container mx-auto mt-6 w-full max-w-384 rounded-2xl bg-card p-3 shadow-md"
    >
      <Highlight
        mode="parent"
        controlledItems
        forceUpdateBounds
        value={activeStep}
        click={false}
        exitDelay={0}
        transition={
          reducedMotion
            ? { duration: 0 }
            : { type: "spring", stiffness: 350, damping: 35 }
        }
        className="pointer-events-none rounded-xl bg-primary-950 shadow-lg ring-2 ring-primary-950"
      >
        <ol className="grid grid-cols-1 gap-2 @xs:grid-cols-2 @xl:grid-cols-3 @[68rem]:grid-cols-6">
          {steps.map((step, index) => {
            const active = step.id === activeStep;
            const number = String(index + 1).padStart(2, "0");
            const stepPending = pending?.[step.id];

            return (
              <li
                key={step.id}
                className="min-w-0 rounded-xl bg-surface-container-low"
              >
                <HighlightItem value={step.id} asChild>
                  <button
                    type="button"
                    aria-current={active ? "step" : undefined}
                    aria-pressed={active}
                    aria-selected={undefined}
                    aria-describedby="review-stepper-status"
                    aria-controls="review-active-panel"
                    onClick={() => onStepChange(step.id)}
                    className="relative z-10 flex h-full min-h-22 w-full items-center gap-2 rounded-xl p-3 text-left outline-none focus-visible:ring-2 focus-visible:ring-on-tertiary-container focus-visible:ring-offset-4 focus-visible:ring-offset-card"
                  >
                    <span
                      aria-hidden="true"
                      className={`flex size-8 shrink-0 items-center justify-center rounded-lg text-sm font-bold leading-5 transition-colors motion-reduce:transition-none ${active ? "bg-card text-primary-950" : "bg-surface-container-highest text-on-surface-variant"}`}
                    >
                      {number}
                    </span>
                    <span className="flex min-w-0 flex-col gap-1">
                      <span
                        className={`text-[10px] font-semibold leading-4 tracking-[0.6px] uppercase ${active ? "text-primary-foreground/70" : "text-on-surface-variant"}`}
                      >
                        Etapa {number}
                        {active ? " (atual)" : ""}
                      </span>
                      <span
                        className={`text-xs font-bold leading-5 ${active ? "text-primary-foreground" : "text-primary-950"}`}
                      >
                        {step.title}
                      </span>
                      <span
                        className={`flex items-center text-xs font-medium leading-4 tracking-[0.3px] ${active ? "text-secondary-fixed" : "text-on-surface-variant"}`}
                      >
                        {stepPending && stepPending.total > 0 ? (
                          <>
                            <span
                              aria-hidden="true"
                              className={cn(
                                "size-1.5 shrink-0 rounded-full bg-secondary-fixed mr-1",
                                active
                                  ? "bg-secondary-fixed"
                                  : "bg-on-surface-variant",
                              )}
                            />
                            <span
                              className={`rounded-full font-bold ${active ? " text-secondary-fixed" : " text-tertiary-900"}`}
                            >
                              {stepPending.total}{" "}
                              {stepPending.total === 1
                                ? "pendência"
                                : "pendências"}
                              {stepPending.relevant > 0
                                ? ` · ${stepPending.relevant} p/ a vaga`
                                : ""}
                            </span>
                          </>
                        ) : clear?.[step.id] ? (
                          <span
                            className={`inline-flex items-center gap-1 rounded-full  font-bold ${active ? "text-secondary-fixed" : "text-tertiary-700"}`}
                          >
                            <BadgeCheck
                              aria-hidden="true"
                              className="size-3.5"
                            />
                            Validado
                          </span>
                        ) : step.id === "preferences" ? (
                          "Ajustes editoriais"
                        ) : (
                          "Aguardando revisão"
                        )}
                      </span>
                    </span>
                  </button>
                </HighlightItem>
              </li>
            );
          })}
        </ol>
      </Highlight>
      <p id="review-stepper-status" aria-live="polite" className="sr-only">
        {steps.find((step) => step.id === activeStep)?.title}: prévia da etapa.
        A revisão estará disponível após o processamento do Currículo Base.
      </p>
    </nav>
  );
}
