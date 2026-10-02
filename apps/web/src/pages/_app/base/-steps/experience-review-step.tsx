import type { CollectionProps, ReviewField } from "@@types/review-model";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const fields: ReviewField[] = [
  { key: "company", label: "Empresa" },
  { key: "role", label: "Cargo oficial" },
  {
    key: "employment",
    label: "Vínculo",
    hint: "Informe o tipo de contratação.",
  },
  {
    key: "status",
    label: "Situação do vínculo",
    hint: "Atual ou encerrado, conforme o documento.",
  },
  { key: "start", label: "Início", type: "month" },
  {
    key: "end",
    label: "Término",
    type: "month",
    hint: "Deixe vazio apenas se o vínculo for atual.",
  },
  {
    key: "description",
    label: "Responsabilidades e contexto",
    type: "textarea",
    wide: true,
  },
];

export function ExperienceReviewStep(props: CollectionProps) {
  return (
    <ReviewStepShell
      number={2}
      title="Experiência Profissional (Núcleo Factual)"
      description="Empresas, cargos oficiais, vínculos e cronologia auditada"
      rule="Confira datas, cargos oficiais e empresas. A adaptação não deve alterar sua senioridade nem presumir que um vínculo continua ativo."
    >
      <ReviewDraftCollection
        {...props}
        title="Histórico profissional"
        itemLabel="Adicionar experiência ao rascunho"
        emptyText="Nenhuma experiência extraída. Os cargos e períodos aparecerão aqui após o processamento do documento."
        fields={fields}
        titleKey="company"
        subtitleKey="role"
      />
    </ReviewStepShell>
  );
}
