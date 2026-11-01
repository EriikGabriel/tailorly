import { AiStageLabel } from "@components/ai-progress/ai-stage-label";
import { DetailsDialog } from "@components/details-dialog";
import { useCurrentAccount } from "@queries/current-account";
import { useViewer } from "@queries/cv-generation";
import { type OwnerVaultSettings, useOwnerVault } from "@queries/owner-vault";
import {
  EVIDENCE_COLLECT_KEY,
  type EvidenceCollection,
  type SupplementaryEvidence,
  useCollectSupplementaryEvidence,
  useReviewSupplementaryEvidence,
  useSupplementaryEvidence,
} from "@queries/supplementary-evidence";
import { useIsMutating } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { EvidenceReviewList, groupBySubject } from "./evidence-review-list";
import { SectionHeading } from "./section-heading";

const COLLECTION_MESSAGE: Record<EvidenceCollection["aiStatus"], string> = {
  completed: "Análise concluída.",
  limit_reached: "Limite de análises atingido. Tente de novo mais tarde.",
  unavailable:
    "Configure um provedor de IA no perfil para analisar os links do seu currículo.",
};

/** De onde as evidências vêm hoje, segundo as preferências do perfil (Second Brain). */
function SourcesLine({
  vault,
  loading,
}: {
  vault: OwnerVaultSettings | undefined;
  loading: boolean;
}) {
  if (loading) return null;
  const notes = vault?.usable
    ? vault.readEnabled
      ? `Notas do vault (${vault.relativePath})`
      : "Notas do vault desligadas"
    : "Sem vault configurado";
  const web = !vault?.webSearchAvailable
    ? "Busca web indisponível no servidor"
    : vault.webSearchEnabled
      ? "Busca web ligada"
      : "Busca web desligada";
  return (
    <p className="text-xs text-on-surface-variant">
      Fontes: links do currículo · {web} · {notes}.{" "}
      <Link
        to="/profile"
        hash="section-connections"
        className="font-medium text-primary underline"
      >
        Configurar no perfil
      </Link>
    </p>
  );
}

/**
 * Detalhes encontrados nos links do Currículo Base, para a pessoa confirmar ou descartar. Só as
 * evidências confirmadas podem entrar no currículo. Exibe-se apenas para contas, porque o snapshot
 * ativo só é conhecido pela conta.
 */

export function SupplementaryEvidenceCard() {
  const viewer = useViewer();
  const account = useCurrentAccount();
  const snapshotId =
    viewer.kind === "account"
      ? (account.data?.owner.activeSnapshotId ?? null)
      : null;
  const evidence = useSupplementaryEvidence(snapshotId, viewer.token);
  const collect = useCollectSupplementaryEvidence(snapshotId, viewer.token);
  // Também vale a coleta automática disparada ao aprovar o Currículo Base.
  const collecting = useIsMutating({ mutationKey: EVIDENCE_COLLECT_KEY }) > 0;
  const review = useReviewSupplementaryEvidence(snapshotId, viewer.token);
  const vault = useOwnerVault(viewer.ownerId, viewer.token);
  const [open, setOpen] = useState(false);
  const [lastCollection, setLastCollection] =
    useState<EvidenceCollection | null>(null);

  if (!snapshotId) return null;

  const items = evidence.data ?? [];
  const confirmed = items.filter((item) => item.reviewState === "confirmado");
  const groups = groupBySubject(items);
  const pendingCount = items.filter(
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
    <div className="flex flex-col gap-3">
      <SectionHeading
        step={4}
        title="Evidências complementares"
        trailing={
          <span className="rounded-full bg-surface-container px-2 py-0.5 font-semibold text-on-surface-variant">
            {confirmed.length} confirmada(s)
          </span>
        }
      />

      <div className="flex flex-col gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-3.75">
        <div className="flex items-center justify-between gap-3">
          <p className="text-[13px] text-on-surface-variant">
            Detalhes dos links de projetos do seu Currículo Base. Só entram no
            currículo se você confirmar.
          </p>
          <button
            type="button"
            onClick={() =>
              collect.mutate(undefined, {
                onSuccess: (result) => setLastCollection(result),
              })
            }
            disabled={collecting}
            className="shrink-0 rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high disabled:opacity-60"
          >
            {collecting ? (
              <AiStageLabel
                orb="searching"
                label="Lendo as páginas dos projetos"
                theme="light"
              />
            ) : (
              "Analisar links"
            )}
          </button>
        </div>

        {lastCollection && !collecting && (
          <p className="text-xs text-on-surface-variant" role="status">
            {COLLECTION_MESSAGE[lastCollection.aiStatus]}{" "}
            {lastCollection.aiStatus === "completed" &&
              `${lastCollection.evidenceCreated} detalhe(s) novo(s).`}
          </p>
        )}
        {collect.isError && (
          <p className="text-xs text-error" role="alert">
            {collect.error.message}
          </p>
        )}

        {evidence.isLoading && (
          <p className="text-xs text-on-surface-variant">
            Carregando evidências...
          </p>
        )}
        {!evidence.isLoading && items.length === 0 && (
          <p className="text-xs text-on-surface-variant">
            Nenhuma evidência ainda. “Analisar links” busca detalhes dos seus
            projetos e experiências em links, na web e no seu second brain.
          </p>
        )}

        <SourcesLine vault={vault.data} loading={vault.isLoading} />

        {items.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-on-surface-variant">
              {pendingCount > 0 ? (
                <strong className="font-semibold text-on-surface">
                  {pendingCount} pendente(s)
                </strong>
              ) : (
                "Nenhuma pendente"
              )}{" "}
              · {confirmed.length} confirmada(s) · {groups.length} item(ns)
            </p>
            <button
              type="button"
              onClick={() => setOpen(true)}
              className="rounded-lg bg-primary-950 px-2.5 py-1.5 text-xs font-medium text-white"
            >
              {pendingCount > 0 ? "Revisar evidências" : "Ver evidências"}
            </button>
          </div>
        )}

        <DetailsDialog
          open={open}
          onOpenChange={setOpen}
          title="Evidências complementares"
          description="Detalhes encontrados em links, na web e no seu second brain. Só os confirmados podem entrar no currículo."
        >
          <EvidenceReviewList
            groups={groups}
            busy={review.isPending}
            onReview={(id, reviewState) => review.mutate({ id, reviewState })}
            onConfirmGroup={confirmGroup}
          />
        </DetailsDialog>
      </div>
    </div>
  );
}
