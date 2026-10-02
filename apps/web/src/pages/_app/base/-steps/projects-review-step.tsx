import type { CollectionProps, ReviewField } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { CheckCheck } from "@react-zero-ui/icon-sprite";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const fields: ReviewField[] = [
  { key: "title", label: "Projeto / realização" },
  { key: "context", label: "Empresa e período" },
  {
    key: "statement",
    label: "Fato e autoria",
    type: "textarea",
    wide: true,
    hint: "Descreva sua participação sem ampliar o escopo da autoria.",
  },
  {
    key: "metric",
    label: "Métrica e unidade",
    hint: "Inclua apenas números documentados.",
  },
  {
    key: "source",
    label: "Referência no documento",
    hint: "Página ou trecho informado por você, ainda não verificado.",
  },
];

export function ProjectsReviewStep(props: CollectionProps) {
  return (
    <ReviewStepShell
      number={3}
      title="Projetos & Realizações (Métricas & Autoria Estrita)"
      description="Fatos quantificados e rastreabilidade de indicadores numéricos"
      rule="Valide números e autoria. Métricas e reduções percentuais precisam de evidência documental; não devem ser inferidas ou infladas."
    >
      <Button
        disabled
        variant="accent"
        size="sm"
        className="self-start rounded-lg text-xs"
        title="A validação depende dos fatos extraídos e de suas evidências"
      >
        <CheckCheck aria-hidden="true" className="size-3.5" />
        Validar todos os fatos
      </Button>
      <ReviewDraftCollection
        {...props}
        title="Fatos e métricas"
        itemLabel="Adicionar realização ao rascunho"
        emptyText="Nenhum fato extraído. As realizações, métricas e referências ao OCR aparecerão aqui após o processamento."
        fields={fields}
        titleKey="title"
        subtitleKey="context"
        evidence
      />
    </ReviewStepShell>
  );
}
