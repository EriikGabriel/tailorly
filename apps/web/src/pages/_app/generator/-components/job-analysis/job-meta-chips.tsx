import type { JobAnalysis, JobPostingResponse } from "@lib/api/generated/model";
import { Briefcase, Clock } from "@react-zero-ui/icon-sprite";
import { formatDate } from "@/utils/date-format";
import {
  JOB_PLATFORM_LABELS,
  SENIORITY_LABELS,
  WORK_MODEL_LABELS,
} from "../../-constants/job-labels";
import { Chip } from "./chip";

/** Etiquetas de plataforma, senioridade, modelo de trabalho, contrato, local e prazo da vaga. */
export function JobMetaChips({
  details,
  analysis,
}: {
  details: JobPostingResponse;
  analysis: JobAnalysis | undefined;
}) {
  const location = [details.location?.city, details.location?.state]
    .filter(Boolean)
    .join(", ");
  const expiresAt = formatDate(details.expiresAt);

  return (
    <div className="flex flex-wrap gap-1.5">
      {details.platform && (
        <Chip>{JOB_PLATFORM_LABELS[details.platform] ?? details.platform}</Chip>
      )}
      {analysis?.seniority && (
        <Chip tone="primary">
          <Briefcase className="size-3" aria-hidden="true" />
          {SENIORITY_LABELS[analysis.seniority] ?? analysis.seniority}
        </Chip>
      )}
      {details.workModel && (
        <Chip>{WORK_MODEL_LABELS[details.workModel] ?? details.workModel}</Chip>
      )}
      {details.employmentType && <Chip>{details.employmentType}</Chip>}
      {location && <Chip>{location}</Chip>}
      {expiresAt && (
        <Chip tone={details.expired ? "warning" : "neutral"}>
          <Clock className="size-3" aria-hidden="true" />
          {details.expired ? "Encerrada em" : "Até"} {expiresAt}
        </Chip>
      )}
    </div>
  );
}
