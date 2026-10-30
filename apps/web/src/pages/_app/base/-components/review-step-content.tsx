import type {
  CollectionProps,
  DraftCollection,
  ReviewStepId,
} from "@@types/review-model";
import { usePresentationStore } from "@stores/presentation-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { EducationReviewStep } from "../-steps/education-review-step";
import { ExperienceReviewStep } from "../-steps/experience-review-step";
import { IdentityReviewStep } from "../-steps/identity-review-step";
import { PreferencesReviewStep } from "../-steps/preferences-review-step";
import { ProjectsReviewStep } from "../-steps/projects-review-step";
import { SkillsReviewStep } from "../-steps/skills-review-step";

/** Formulário da etapa ativa, ligado ao rascunho de revisão. */
export function ReviewStepContent({ step }: { step: ReviewStepId }) {
  const { sourceId, draft, setIdentity, setCollection } = useReviewDraftStore();
  const { preferences, setPreferences } = usePresentationStore();

  function collection(key: DraftCollection): CollectionProps {
    return {
      collectionKey: key,
      records: draft[key],
      onChange: (records) => setCollection(sourceId, key, records),
    };
  }

  switch (step) {
    case "identity":
      return (
        <IdentityReviewStep
          values={draft.identity}
          onChange={(identity) => setIdentity(sourceId, identity)}
        />
      );
    case "experience":
      return (
        <ExperienceReviewStep
          experience={collection("experience")}
          volunteering={collection("volunteering")}
        />
      );
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
        <PreferencesReviewStep value={preferences} onChange={setPreferences} />
      );
  }
}
