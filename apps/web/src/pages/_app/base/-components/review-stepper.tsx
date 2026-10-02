import { type ReviewStepId, reviewSteps as steps } from "@@types/review-model";
import {
  Highlight,
  HighlightItem,
} from "@animate/primitives/effects/highlight";
import { useReducedMotion } from "motion/react";

export function ReviewStepper({
  activeStep,
  onStepChange,
}: {
  activeStep: ReviewStepId;
  onStepChange: (step: ReviewStepId) => void;
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
                    <span className="flex min-w-0 flex-col">
                      <span
                        className={`text-[10px] font-semibold leading-4 tracking-[0.6px] uppercase ${active ? "text-primary-foreground/70" : "text-on-surface-variant"}`}
                      >
                        Etapa {number}
                        {active ? " (atual)" : ""}
                      </span>
                      <span
                        className={`text-sm font-bold leading-5 ${active ? "text-primary-foreground" : "text-primary-950"}`}
                      >
                        {step.title}
                      </span>
                      <span
                        className={`flex items-center gap-1 text-xs font-medium leading-4 tracking-[0.3px] ${active ? "text-secondary-fixed" : "text-on-surface-variant"}`}
                      >
                        {active && (
                          <span
                            aria-hidden="true"
                            className="size-2 shrink-0 rounded-full bg-secondary-fixed"
                          />
                        )}
                        {step.id === "preferences"
                          ? "Ajustes editoriais"
                          : "Aguardando revisão"}
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
