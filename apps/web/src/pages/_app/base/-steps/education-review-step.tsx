import type { CollectionProps, ReviewField } from "@@types/review-model";
import {
  Award,
  BadgeCheck,
  Building2,
  GraduationCap,
} from "@react-zero-ui/icon-sprite";
import {
  summarizeCertificate,
  summarizeDegree,
} from "../-components/record-summaries";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const degreeFields: ReviewField[] = [
  {
    key: "degree",
    label: "Curso / grau acadêmico",
    icon: GraduationCap,
    wide: true,
  },
  { key: "institution", label: "Instituição", icon: Building2, wide: true },
  {
    key: "period",
    label: "Período",
    type: "period",
    period: {
      startKey: "start",
      endKey: "end",
      statusKey: "status",
      startLabel: "Início",
      endLabel: "Conclusão",
      currentLabel: "Curso em andamento",
      currentStatus: "Em andamento",
      endedStatus: "Concluído",
    },
  },
];
const certificateFields: ReviewField[] = [
  { key: "name", label: "Certificação", icon: Award, wide: true },
  { key: "issuer", label: "Órgão emissor", icon: Building2 },
  {
    key: "credential",
    label: "Credencial / ID",
    icon: BadgeCheck,
    hint: "Código ou identificação da credencial, se constar no documento.",
  },
  {
    key: "period",
    label: "Emissão e validade",
    type: "period",
    period: {
      startKey: "issued",
      endKey: "expires",
      startLabel: "Emissão",
      endLabel: "Validade",
      futureEnd: true,
    },
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
      description="Cursos, instituições e credenciais"
      rule="Confira os títulos e as credenciais. A validade de uma certificação não é presumida quando ausente."
    >
      <ReviewDraftCollection
        {...degrees}
        title="Formação acadêmica"
        itemLabel="Adicionar formação"
        emptyText="Nenhuma formação extraída do documento."
        fields={degreeFields}
        summarize={summarizeDegree}
        icon={GraduationCap}
      />
      <ReviewDraftCollection
        {...certifications}
        title="Certificações"
        itemLabel="Adicionar certificação"
        emptyText="Nenhuma certificação extraída do documento."
        fields={certificateFields}
        summarize={summarizeCertificate}
        icon={Award}
      />
    </ReviewStepShell>
  );
}
