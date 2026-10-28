import type { CollectionProps } from "@@types/review-model";
import { LanguageList } from "../-components/language-list";
import { ReviewStepShell } from "../-components/review-step-shell";
import { TechnologyChips } from "../-components/technology-chips";

export function SkillsReviewStep({
  languages,
  technologies,
}: {
  languages: CollectionProps;
  technologies: CollectionProps;
}) {
  return (
    <ReviewStepShell
      number={5}
      title="Competências & Idiomas"
      description="Tecnologias, ferramentas e idiomas"
      rule="O nível de proficiência só aparece quando informado ou atestado por você, sem presunção pela IA."
    >
      <LanguageList {...languages} />
      <TechnologyChips {...technologies} />
    </ReviewStepShell>
  );
}
