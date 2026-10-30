import { type ReviewStepId, reviewSteps } from "@@types/review-model";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { ReviewStepContent } from "./review-step-content";

/** Painel da etapa ativa, com transição suave (e sem movimento para quem prefere menos animação). */
export function ReviewStepPanel({ step }: { step: ReviewStepId }) {
  const reducedMotion = useReducedMotion();
  const title = reviewSteps.find((item) => item.id === step)?.title;

  return (
    <section id="review-active-panel" aria-label={title}>
      <AutoHeight
        deps={[step]}
        transition={reducedMotion ? { duration: 0 } : undefined}
      >
        <AnimatePresence initial={false} mode="wait">
          <motion.div
            key={step}
            initial={{ opacity: 0, y: reducedMotion ? 0 : 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: reducedMotion ? 0 : -8 }}
            transition={{ duration: reducedMotion ? 0 : 0.18 }}
            className="p-0.5"
          >
            <ReviewStepContent step={step} />
          </motion.div>
        </AnimatePresence>
      </AutoHeight>
    </section>
  );
}
