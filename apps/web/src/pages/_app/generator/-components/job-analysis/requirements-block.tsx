import type { JobAnalysis, JobPostingResponse } from "@lib/api/generated/model";
import { groupRequirements } from "../../-utils/job-requirements";
import { RequirementList } from "./requirement-list";

/** Requisitos da vaga, da análise por IA ou, sem ela, da divisão do texto por títulos. */
export function RequirementsBlock({
  details,
  analysis,
}: {
  details: JobPostingResponse;
  analysis: JobAnalysis | undefined;
}) {
  const grouped = groupRequirements(details, analysis);

  if (grouped.required.length === 0 && grouped.preferred.length === 0) {
    return (
      <p className="text-[13px] text-on-surface-variant">
        Nenhum requisito foi identificado no texto da vaga.
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-3">
      <RequirementList title="Obrigatórios" items={grouped.required} />
      <RequirementList title="Desejáveis" items={grouped.preferred} />
    </div>
  );
}
