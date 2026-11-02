import { PageHeading } from "@components/page-heading";
import { useSecondBrain } from "@queries/second-brain";
import {
  Link as LinkIcon,
  Network,
  Sparkles,
} from "@react-zero-ui/icon-sprite";

export function WorkspaceHeader({ baseCvFile }: { baseCvFile: File | null }) {
  const statusLabel = baseCvFile
    ? "Ground Truth configurado"
    : "Aguardando Currículo Base";
  const { status } = useSecondBrain();
  const secondBrainLabel = !status.connected
    ? "Desconectado - Opcional"
    : status.readsNotes
      ? "Conectado · Busca usa o vault"
      : "Conectado · Leitura de notas desligada";

  return (
    <PageHeading
      headingId="generator-heading"
      breadcrumb="Gerador de Currículos"
      status={statusLabel}
      eyebrowIcon={Sparkles}
      eyebrow="Geração assistida por IA"
      title="Gerador de Currículo Sob Medida"
      description={
        <>
          Alimentado por IA e integrado ao seu Second Brain (Obsidian) para
          extrair e sintetizar suas experiências de maior impacto para{" "}
          <strong className="font-semibold text-primary-950">
            cada vaga específica
          </strong>
          .
        </>
      }
      actions={
        <div className="flex flex-wrap items-center gap-2 rounded-xl border border-outline-variant/60 bg-card px-3.5 py-2.5 shadow-xs">
          <span className="inline-flex items-center gap-1.5 pr-2">
            <LinkIcon
              aria-hidden="true"
              className="size-3.5 text-on-surface-variant"
            />
            <span className="text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface">
              Fonte Principal (Ground Truth):
            </span>
            <span className="rounded bg-secondary-container px-2 py-0.5 text-xs font-semibold leading-4 tracking-[0.3px] text-on-secondary-container">
              {baseCvFile ? baseCvFile.name : "Nenhum arquivo"}
              {baseCvFile && " (Ativo)"}
            </span>
          </span>
          <span aria-hidden="true" className="text-outline-variant">
            •
          </span>
          <span className="inline-flex items-center gap-1.5">
            <Network
              aria-hidden="true"
              className="size-3.5 text-on-surface-variant"
            />
            <span className="text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface-variant">
              Second Brain ({secondBrainLabel})
            </span>
          </span>
        </div>
      }
    />
  );
}
