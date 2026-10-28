import { FileCheck2, ShieldCheck, SquarePen } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { Link } from "@tanstack/react-router";
import { SectionHeading } from "./section-heading";

export function GroundTruthCard() {
  const { file, selectionId, uploadedSelectionId } = useBaseCvStore();
  const isSent = Boolean(
    file && selectionId && uploadedSelectionId === selectionId,
  );

  return (
    <div className="flex flex-col gap-3 border-b border-outline-variant/60 pb-4">
      <SectionHeading
        step={1}
        title="Ground Truth: Currículo Base"
        trailing={
          <span className="inline-flex items-center gap-1 rounded-full bg-secondary-container px-2 py-0.5 font-semibold tracking-[0.3px] text-on-secondary-container">
            <ShieldCheck className="size-3" aria-hidden="true" />
            100% Fatos Reais
          </span>
        }
      />

      <div className="flex items-center justify-between gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-3.75">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-surface-container">
            <FileCheck2
              aria-hidden="true"
              className={`size-5 ${isSent ? "text-on-tertiary-container" : "text-on-surface-variant"}`}
            />
          </span>
          <div className="min-w-0">
            <p
              className="truncate text-sm font-bold tracking-[0.14px] text-on-surface"
              title={file?.name}
            >
              {file ? file.name : "Nenhum Currículo Base configurado"}
            </p>
            <p className="truncate text-[13px] text-on-surface-variant">
              {file
                ? isSent
                  ? "Ground Truth ativo para esta geração"
                  : "Prévia local • ainda não enviado"
                : "Envie e revise seu currículo base antes de gerar"}
            </p>
          </div>
        </div>
        <Link
          to="/base"
          className="shrink-0 rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        >
          <span className="inline-flex items-center gap-1">
            <SquarePen className="size-3" aria-hidden="true" />
            {file ? "Alterar / Editar Base" : "Configurar Base"}
          </span>
        </Link>
      </div>
    </div>
  );
}
