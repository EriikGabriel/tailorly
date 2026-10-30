import { FileText, ListChecks, Shield } from "@react-zero-ui/icon-sprite";
import { formatFileSizeKb } from "../-utils/file-status";
import { GovernanceCard } from "./governance-card";

/** Três cartões de governança: arquivo mestre, índice de confiabilidade e integridade. */
export function GovernanceSummary({
  file,
  fileStatus,
}: {
  file: File | null;
  fileStatus: string;
}) {
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
        <p className="flex flex-wrap items-baseline gap-1 pb-1">
          <span className="text-xl font-bold leading-7 text-primary-950">
            —
          </span>
          <span className="text-body-sm font-medium text-muted-foreground">
            Ainda não avaliado
          </span>
        </p>
        <div
          aria-hidden="true"
          className="h-1.5 rounded-full bg-surface-container-highest"
        />
      </GovernanceCard>
      <GovernanceCard
        label="Protocolo de integridade"
        icon={<Shield aria-hidden="true" className="size-5" />}
      >
        <p className="text-title-md text-primary-950">Aguardando revisão</p>
        <p className="text-body-sm">Nenhum fato confirmado</p>
      </GovernanceCard>
    </div>
  );
}
