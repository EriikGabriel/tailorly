import type { CollectionProps, ReviewField } from "@@types/review-model";
import { Briefcase, Building2, HandHeart } from "@react-zero-ui/icon-sprite";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";
import {
  summarizeExperience,
  summarizeVolunteering,
} from "../-utils/record-summaries";

const employmentOptions = [
  "CLT",
  "PJ",
  "Estágio",
  "Trainee",
  "Aprendiz",
  "Temporário",
  "Autônomo / Freelancer",
].map((value) => ({ value, label: value }));

const experienceFields: ReviewField[] = [
  {
    key: "role",
    label: "Cargo",
    icon: Briefcase,
    hint: "Cargo oficial, como no documento.",
  },
  { key: "company", label: "Empresa", icon: Building2 },
  {
    key: "employment",
    label: "Vínculo",
    type: "select",
    options: employmentOptions,
    icon: Briefcase,
    hint: "Tipo de contratação informado no documento.",
  },
  {
    key: "period",
    label: "Período",
    type: "period",
    period: {
      startKey: "start",
      endKey: "end",
      statusKey: "status",
      startLabel: "Início",
      endLabel: "Término",
      currentLabel: "Trabalho aqui atualmente",
      currentStatus: "Atual",
      endedStatus: "Encerrado",
    },
  },
  {
    key: "description",
    label: "Responsabilidades e contexto",
    type: "textarea",
    wide: true,
    maxLength: 1500,
    hint: "Texto do documento sobre o que foi feito neste cargo.",
  },
];

const volunteeringFields: ReviewField[] = [
  {
    key: "organization",
    label: "Organização ou causa",
    icon: Building2,
    hint: "ONG, instituição ou iniciativa em que atuou.",
  },
  {
    key: "role",
    label: "Função",
    icon: HandHeart,
    hint: "Papel desempenhado na atividade voluntária.",
  },
  {
    key: "period",
    label: "Período",
    type: "period",
    period: {
      startKey: "start",
      endKey: "end",
      statusKey: "status",
      startLabel: "Início",
      endLabel: "Término",
      currentLabel: "Atuo como voluntário atualmente",
      currentStatus: "Atual",
      endedStatus: "Encerrado",
    },
  },
  {
    key: "description",
    label: "Atividades realizadas",
    type: "textarea",
    wide: true,
    maxLength: 1000,
    hint: "Texto do documento sobre o trabalho voluntário.",
  },
];

export function ExperienceReviewStep({
  experience,
  volunteering,
}: {
  experience: CollectionProps;
  volunteering: CollectionProps;
}) {
  return (
    <ReviewStepShell
      number={2}
      title="Experiência & Voluntariado"
      description="Cargos remunerados e atividades voluntárias"
      rule="Confira datas, cargos e organizações. A adaptação não altera sua senioridade, não presume que um vínculo continua ativo e não mistura voluntariado com emprego."
    >
      <ReviewDraftCollection
        {...experience}
        title="Histórico profissional"
        itemLabel="Adicionar experiência"
        emptyText="Nenhuma experiência profissional extraída. Os cargos e períodos aparecem aqui depois do processamento do documento."
        fields={experienceFields}
        summarize={summarizeExperience}
        icon={Briefcase}
      />
      <ReviewDraftCollection
        {...volunteering}
        title="Atividades voluntárias"
        itemLabel="Adicionar voluntariado"
        emptyText="Nenhuma atividade voluntária extraída do documento."
        fields={volunteeringFields}
        summarize={summarizeVolunteering}
        icon={HandHeart}
      />
    </ReviewStepShell>
  );
}
