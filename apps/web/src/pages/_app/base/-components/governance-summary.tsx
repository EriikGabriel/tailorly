import { AnimatedBar } from "@components/animated-bar";
import { useBaseReview } from "@queries/base-review";
import { useActiveGroundTruth } from "@queries/ground-truth";
import { FileText, ListChecks, Shield } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import {
  integrityState,
  summarizeGovernance,
} from "@utils/review/base-governance";
import { formatFileSizeKb } from "../-utils/file-status";
import { GovernanceCard } from "./governance-card";

const TONE_CLASS = {
  ok: "text-on-tertiary-container",
  warning: "text-error",
  idle: "text-primary-950",
} as const;

/**
 * Três cartões de governança: arquivo mestre, índice de confiabilidade e integridade. Os dois
 * últimos são calculados dos fatos e pendências reais da versão; sem versão, ficam em espera.
 */
export function GovernanceSummary({
  file,
  fileStatus,
}: {
  file: File | null;
  fileStatus: string;
}) {
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const { records, issues } = useBaseReview(baseVersionId ?? undefined);
  const { version } = useActiveGroundTruth();
  const loading =
    Boolean(baseVersionId) && (records.isLoading || issues.isLoading);
  const summary = summarizeGovernance(records.data ?? [], issues.data ?? []);
  const integrity = integrityState(
    summary,
    Boolean(baseVersionId) && version?.id === baseVersionId,
    loading,
  );

  return (
    <div className="grid gap-4 md:grid-cols-3">
      <GovernanceCard
        label="Arquivo mestre"
        icon={<FileText aria-hidden="true" className="size-5" />}
      >
        <p
          className="truncate text-title-md text-primary-950"
          title={file?.name}
        >
          {file?.name ?? "Nenhum arquivo selecionado"}
        </p>
        <p className="text-body-sm">
          {file
            ? `${formatFileSizeKb(file)} • ${fileStatus}`
            : "Selecione seu currículo em PDF"}
        </p>
      </GovernanceCard>
      <GovernanceCard
        label="Índice de confiabilidade"
        accent
        icon={<ListChecks aria-hidden="true" className="size-5" />}
      >
        <p className="flex flex-wrap items-baseline gap-3 pb-1">
          <span className="text-xl font-bold leading-7 text-primary-950">
            {summary.reliability === null ? "—" : `${summary.reliability}%`}
          </span>
          <span className="text-body-sm font-medium text-muted-foreground">
            {summary.reliability === null
              ? "Ainda não avaliado"
              : `${summary.reviewed} de ${summary.considered} fatos revisados`}
          </span>
        </p>
        <AnimatedBar
          value={summary.reliability ?? 0}
          max={100}
          label="Índice de confiabilidade"
        />
      </GovernanceCard>
      <GovernanceCard
        label="Protocolo de integridade"
        icon={<Shield aria-hidden="true" className="size-5" />}
      >
        <p className={`text-title-md ${TONE_CLASS[integrity.tone]}`}>
          {integrity.title}
        </p>
        <p className="text-body-sm">{integrity.detail}</p>
      </GovernanceCard>
    </div>
  );
}
