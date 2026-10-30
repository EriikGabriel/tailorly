import { type ReviewStepId, reviewSteps } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { ArrowLeft, ArrowRight } from "@react-zero-ui/icon-sprite";

/** Botões "Anterior" e "Próxima" entre as etapas da revisão. */
export function ReviewStepNavigation({
  activeStep,
  onStepChange,
}: {
  activeStep: ReviewStepId;
  onStepChange: (step: ReviewStepId) => void;
}) {
  const stepIndex = reviewSteps.findIndex((step) => step.id === activeStep);

  return (
    <nav
      aria-label="Navegação entre etapas"
      className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-card border p-4"
    >
      <Button
        variant="accent"
        disabled={stepIndex === 0}
        onClick={() => onStepChange(reviewSteps[stepIndex - 1].id)}
        className="rounded-lg"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Anterior
      </Button>
      <p aria-live="polite" className="text-xs font-medium">
        Etapa {stepIndex + 1} de {reviewSteps.length}
      </p>
      <Button
        disabled={stepIndex === reviewSteps.length - 1}
        onClick={() => onStepChange(reviewSteps[stepIndex + 1].id)}
        className="rounded-lg"
      >
        Próxima
        <ArrowRight aria-hidden="true" className="size-4" />
      </Button>
    </nav>
  );
}
