import {
  type CollectionProps,
  type DraftCollection,
  reviewSteps,
} from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { ArrowLeft, ArrowRight } from "@react-zero-ui/icon-sprite";
import { usePresentationStore } from "@stores/presentation-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { EducationReviewStep } from "../-steps/education-review-step";
import { ExperienceReviewStep } from "../-steps/experience-review-step";
import { IdentityReviewStep } from "../-steps/identity-review-step";
import { PreferencesReviewStep } from "../-steps/preferences-review-step";
import { ProjectsReviewStep } from "../-steps/projects-review-step";
import { SkillsReviewStep } from "../-steps/skills-review-step";
import { MasterDocumentPanel } from "./master-document-panel";
import { ReviewCompletionCard } from "./review-completion-card";
import { ReviewStepper } from "./review-stepper";

export function ReviewWorkspace({
  onSelectFile,
}: {
  onSelectFile: () => void;
}) {
  const sourceId = useReviewDraftStore((state) => state.sourceId);
  const activeStep = useReviewDraftStore((state) => state.activeStep);
  const setActiveStep = useReviewDraftStore((state) => state.setActiveStep);
  const draft = useReviewDraftStore((state) => state.draft);
  const setIdentity = useReviewDraftStore((state) => state.setIdentity);
  const setCollection = useReviewDraftStore((state) => state.setCollection);
  const preferences = usePresentationStore((state) => state.preferences);
  const setPreferences = usePresentationStore((state) => state.setPreferences);
  const reducedMotion = useReducedMotion();

  function collection(key: DraftCollection): CollectionProps {
    return {
      collectionKey: key,
      records: draft[key],
      onChange: (records) => setCollection(sourceId, key, records),
    };
  }

  function renderStep() {
    switch (activeStep) {
      case "identity":
        return (
          <IdentityReviewStep
            values={draft.identity}
            onChange={(identity) => setIdentity(sourceId, identity)}
          />
        );
      case "experience":
        return <ExperienceReviewStep {...collection("experience")} />;
      case "projects":
        return <ProjectsReviewStep {...collection("projects")} />;
      case "education":
        return (
          <EducationReviewStep
            degrees={collection("degrees")}
            certifications={collection("certifications")}
          />
        );
      case "skills":
        return (
          <SkillsReviewStep
            languages={collection("languages")}
            technologies={collection("technologies")}
          />
        );
      case "preferences":
        return (
          <PreferencesReviewStep
            value={preferences}
            onChange={setPreferences}
          />
        );
    }
  }

  const stepIndex = reviewSteps.findIndex((step) => step.id === activeStep);
  return (
    <>
      <ReviewStepper activeStep={activeStep} onStepChange={setActiveStep} />
      <div className="mx-auto mt-6 grid w-full max-w-384 items-start gap-6 xl:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <MasterDocumentPanel onSelectFile={onSelectFile} />
        <div className="min-w-0 space-y-4">
          <section
            id="review-active-panel"
            aria-label={reviewSteps[stepIndex].title}
          >
            <AutoHeight
              deps={[activeStep]}
              transition={reducedMotion ? { duration: 0 } : undefined}
            >
              <AnimatePresence initial={false} mode="wait">
                <motion.div
                  key={activeStep}
                  initial={{ opacity: 0, y: reducedMotion ? 0 : 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: reducedMotion ? 0 : -8 }}
                  transition={{ duration: reducedMotion ? 0 : 0.18 }}
                  className="p-0.5"
                >
                  {renderStep()}
                </motion.div>
              </AnimatePresence>
            </AutoHeight>
          </section>
          <nav
            aria-label="Navegação entre etapas"
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-card border p-4"
          >
            <Button
              variant="accent"
              disabled={stepIndex === 0}
              onClick={() => setActiveStep(reviewSteps[stepIndex - 1].id)}
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
              onClick={() => setActiveStep(reviewSteps[stepIndex + 1].id)}
              className="rounded-lg"
            >
              Próxima
              <ArrowRight aria-hidden="true" className="size-4" />
            </Button>
          </nav>
          {activeStep === "preferences" && <ReviewCompletionCard />}
        </div>
      </div>
    </>
  );
}
