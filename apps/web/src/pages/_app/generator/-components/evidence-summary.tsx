import { AiStageLabel } from "@components/ai-progress/ai-stage-label";
import { DetailsDialog } from "@components/details-dialog";
import {
  EvidenceReviewList,
  groupBySubject,
} from "@components/evidence/evidence-review-list";
import type { OwnerVaultSettings } from "@queries/owner-vault";
import { useSecondBrain } from "@queries/second-brain";
import {
  EVIDENCE_COLLECT_KEY,
  type SupplementaryEvidence,
  useCollectSupplementaryEvidence,
  useEvidenceSnapshot,
  useReviewSupplementaryEvidence,
  useSupplementaryEvidence,
} from "@queries/supplementary-evidence";
import { useIsMutating } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { useState } from "react";

/**
 * De onde as evidências vêm hoje, segundo as preferências do perfil (Second Brain). Só cita as
 * fontes que o servidor oferece (vault e busca web podem não existir em produção).
 */
function SourcesLine({ vault }: { vault: OwnerVaultSettings | undefined }) {
  const sources = ["links do currículo"];
  if (vault?.webSearchAvailable) {
    sources.push(
      vault.webSearchEnabled ? "busca web ligada" : "busca web desligada",
    );
  }
  if (vault?.rootAvailable) {
    sources.push(
      vault.usable
        ? vault.readEnabled
          ? `notas do vault (${vault.relativePath})`
          : "notas do vault desligadas"
        : "sem vault configurado",
    );
  }
  const configurable = sources.length > 1;
  return (
    <p className="text-xs text-on-surface-variant">
      Fontes: {sources.join(" · ")}.
      {configurable && (
        <>
          {" "}
          <Link
            to="/profile"
            hash="section-connections"
            className="font-medium text-primary underline"
          >
            Configurar no perfil
          </Link>
        </>
      )}
    </p>
  );
}

/**
 * Resumo das evidências complementares no rodapé da geração: quantas foram confirmadas e uma
 * entrada para ver, mudar a decisão ou buscar novas. A revisão em si acontece no diálogo antes de
 * gerar; aqui a lista só aparece quando a conta já tem alguma evidência.
 */
export function EvidenceSummary() {
  const { snapshotId, token } = useEvidenceSnapshot();
  const evidence = useSupplementaryEvidence(snapshotId, token);
  const review = useReviewSupplementaryEvidence(snapshotId, token);
  const collect = useCollectSupplementaryEvidence(snapshotId, token);
  const collecting = useIsMutating({ mutationKey: EVIDENCE_COLLECT_KEY }) > 0;
  const { settings } = useSecondBrain();
  const [open, setOpen] = useState(false);

  const items = evidence.data ?? [];
  if (!snapshotId || items.length === 0) return null;

  const confirmed = items.filter(
    (item) => item.reviewState === "confirmado",
  ).length;
  const pending = items.filter(
    (item) => item.reviewState === "pendente",
  ).length;

  async function confirmGroup(group: SupplementaryEvidence[]) {
    for (const item of group.filter(
      (entry) => entry.reviewState === "pendente",
    )) {
      await review.mutateAsync({ id: item.id, reviewState: "confirmado" });
    }
  }

  return (
    <div className="mt-4 flex w-full flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-3 py-2 text-xs text-on-surface-variant">
      <span>
        Evidências complementares: {confirmed} confirmada(s)
        {pending > 0 ? ` · ${pending} pendente(s) fora do currículo` : ""}
      </span>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="font-semibold text-primary underline"
      >
        Ver evidências
      </button>
      <DetailsDialog
        open={open}
        onOpenChange={setOpen}
        title="Evidências complementares"
        description="Detalhes encontrados em links, na web e no seu second brain. Só os confirmados podem entrar no currículo, na próxima geração."
      >
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <SourcesLine vault={settings} />
            <button
              type="button"
              onClick={() => collect.mutate()}
              disabled={collecting}
              className="shrink-0 rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high disabled:opacity-60"
            >
              {collecting ? (
                <AiStageLabel
                  orb="searching"
                  label="Buscando evidências"
                  theme="light"
                />
              ) : (
                "Buscar novas"
              )}
            </button>
          </div>
          {collect.isError && (
            <p className="text-xs text-error" role="alert">
              {collect.error.message}
            </p>
          )}
          <EvidenceReviewList
            groups={groupBySubject(items)}
            busy={review.isPending}
            onReview={(id, reviewState) => review.mutate({ id, reviewState })}
            onConfirmGroup={confirmGroup}
          />
        </div>
      </DetailsDialog>
    </div>
  );
}
