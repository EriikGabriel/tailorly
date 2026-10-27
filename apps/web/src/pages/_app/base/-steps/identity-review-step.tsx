import type { DraftFields, ReviewField } from "@@types/review-model";
import { ReviewDraftFields } from "../-components/review-draft-fields";
import { ReviewStepShell } from "../-components/review-step-shell";

const identityFields: ReviewField[] = [
  {
    key: "name",
    label: "Nome profissional completo",
    hint: "Use o nome presente no documento.",
  },
  {
    key: "headline",
    label: "Cargo / título profissional",
    hint: "Título exibido no topo do currículo, se houver.",
  },
  {
    key: "location",
    label: "Localização geográfica",
    hint: "Cidade, estado e país informados.",
  },
  {
    key: "email",
    label: "E-mail principal",
    type: "email",
    hint: "Canal de contato direto.",
  },
  {
    key: "phone",
    label: "Telefone / WhatsApp",
    type: "tel",
    hint: "Inclua os códigos de país e região.",
  },
  {
    key: "summary",
    label: "Resumo profissional",
    type: "textarea",
    wide: true,
    hint: "Apenas o texto de apresentação que consta no documento.",
  },
];
const linkFields: ReviewField[] = [
  { key: "linkedin", label: "LinkedIn", type: "url" },
  { key: "github", label: "GitHub / Portfólio", type: "url" },
  { key: "website", label: "Site pessoal", type: "url" },
];

export function IdentityReviewStep({
  values,
  onChange,
}: {
  values: DraftFields;
  onChange: (values: DraftFields) => void;
}) {
  return (
    <ReviewStepShell
      number={1}
      title="Identificação e Contato"
      description="Dados oficiais do candidato e links de curadoria pública"
      rule="Nome profissional e ao menos um canal de contato direto válidos são obrigatórios antes de exportar."
    >
      <ReviewDraftFields
        fields={identityFields}
        values={values}
        onChange={onChange}
      />
      <h3 className="text-xs font-semibold tracking-[0.6px] uppercase">
        Links públicos para curadoria
      </h3>
      <ReviewDraftFields
        fields={linkFields}
        values={values}
        onChange={onChange}
      />
    </ReviewStepShell>
  );
}
