import type { CollectionProps, ReviewField } from "@@types/review-model";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const degreeFields: ReviewField[] = [
  { key: "degree", label: "Grau acadêmico / curso", wide: true },
  { key: "institution", label: "Instituição" },
  {
    key: "status",
    label: "Situação",
    hint: "Concluído, em andamento ou interrompido.",
  },
  { key: "start", label: "Início", type: "month" },
  { key: "end", label: "Conclusão", type: "month" },
];
const certificateFields: ReviewField[] = [
  { key: "name", label: "Nome da certificação", wide: true },
  { key: "issuer", label: "Órgão emissor" },
  { key: "credential", label: "Credencial / identificação" },
  { key: "issued", label: "Emissão", type: "month" },
  {
    key: "expires",
    label: "Validade",
    type: "month",
    hint: "Não presume validade quando ausente.",
  },
];

export function EducationReviewStep({
  degrees,
  certifications,
}: {
  degrees: CollectionProps;
  certifications: CollectionProps;
}) {
  return (
    <ReviewStepShell
      number={4}
      title="Formação & Certificações"
      description="Títulos acadêmicos, órgãos emissores e credenciais oficiais"
      rule="Confira os requisitos acadêmicos e as credenciais. Certificações e sua validade devem corresponder às informações fornecidas."
    >
      <ReviewDraftCollection
        {...degrees}
        title="Grau acadêmico"
        itemLabel="Adicionar formação"
        emptyText="Nenhuma formação extraída do documento."
        fields={degreeFields}
        titleKey="degree"
        subtitleKey="institution"
      />
      <ReviewDraftCollection
        {...certifications}
        title="Certificações técnicas"
        itemLabel="Adicionar certificação"
        emptyText="Nenhuma certificação extraída do documento."
        fields={certificateFields}
        titleKey="name"
        subtitleKey="issuer"
      />
    </ReviewStepShell>
  );
}
