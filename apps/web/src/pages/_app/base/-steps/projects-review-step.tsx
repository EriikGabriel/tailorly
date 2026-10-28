import type { CollectionProps, ReviewField } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import {
  Building2,
  CheckCheck,
  FolderBookmark,
  Quote,
  TrendingUp,
} from "@react-zero-ui/icon-sprite";
import { summarizeProject } from "../-components/record-summaries";
import { ReviewDraftCollection } from "../-components/review-draft-collection";
import { ReviewStepShell } from "../-components/review-step-shell";

const fields: ReviewField[] = [
  { key: "title", label: "Projeto ou realização", icon: FolderBookmark },
  {
    key: "context",
    label: "Contexto",
    icon: Building2,
    placeholder: "Empresa, curso ou período",
  },
  {
    key: "statement",
    label: "O que foi feito",
    type: "textarea",
    wide: true,
    maxLength: 800,
    hint: "Descreva sua participação sem ampliar o escopo da autoria.",
  },
  {
    key: "metric",
    label: "Métrica",
    icon: TrendingUp,
    placeholder: "ex.: 35% · 300 alunos",
    hint: "Inclua apenas números documentados.",
  },
  { key: "link", label: "Link do projeto", type: "url" },
  {
    key: "source",
    label: "Referência no documento",
    icon: Quote,
    wide: true,
    hint: "Página ou trecho informado por você, ainda não verificado.",
  },
];

export function ProjectsReviewStep(props: CollectionProps) {
  return (
    <ReviewStepShell
      number={3}
      title="Projetos & Realizações"
      description="Fatos quantificados, links e autoria"
      rule="Valide números e autoria. Métricas precisam de evidência no documento e não devem ser inferidas ou infladas."
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
        itemLabel="Adicionar realização"
        emptyText="Nenhum fato extraído. As realizações, métricas e referências aparecem aqui depois do processamento."
        fields={fields}
        summarize={summarizeProject}
        icon={FolderBookmark}
        evidence
      />
    </ReviewStepShell>
  );
}
