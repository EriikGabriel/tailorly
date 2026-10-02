import type { CollectionProps, ReviewField } from "@@types/review-model";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const languageFields: ReviewField[] = [
  { key: "language", label: "Idioma" },
  {
    key: "proficiency",
    label: "Proficiência",
    hint: "Somente o nível informado ou atestado.",
  },
];
const technologyFields: ReviewField[] = [
  { key: "name", label: "Tecnologia / competência" },
  {
    key: "context",
    label: "Contexto de uso",
    hint: "Experiência ou projeto em que foi utilizada.",
  },
];

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
      description="Ferramentas, tecnologias, métodos e idiomas com proficiência estrita"
      rule="Exiba nível de proficiência somente quando informado ou atestado por você, sem presunção pela IA."
    >
      <ReviewDraftCollection
        {...languages}
        title="Idiomas do dossiê"
        itemLabel="Adicionar idioma"
        emptyText="Nenhum idioma informado. Não presumimos sua proficiência."
        fields={languageFields}
        titleKey="language"
        subtitleKey="proficiency"
      />
      <ReviewDraftCollection
        {...technologies}
        title="Stack tecnológica"
        itemLabel="Inserir tecnologia manual"
        emptyText="Nenhuma tecnologia extraída. As competências precisam estar ligadas à sua experiência real."
        fields={technologyFields}
        titleKey="name"
        subtitleKey="context"
      />
    </ReviewStepShell>
  );
}
