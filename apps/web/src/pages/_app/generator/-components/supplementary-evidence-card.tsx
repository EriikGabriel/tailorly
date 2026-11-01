import { AiStageLabel } from "@components/ai-progress/ai-stage-label";
import { useCurrentAccount } from "@queries/current-account";
import { useViewer } from "@queries/cv-generation";
import {
  EVIDENCE_COLLECT_KEY,
  type EvidenceCollection,
  type SupplementaryEvidence,
  useCollectSupplementaryEvidence,
  useReviewSupplementaryEvidence,
  useSupplementaryEvidence,
} from "@queries/supplementary-evidence";
import { useIsMutating } from "@tanstack/react-query";
import { useState } from "react";
import { SectionHeading } from "./section-heading";

const COLLECTION_MESSAGE: Record<EvidenceCollection["aiStatus"], string> = {
  completed: "Análise concluída.",
  limit_reached: "Limite de análises atingido. Tente de novo mais tarde.",
  unavailable:
    "Configure um provedor de IA no perfil para analisar os links do seu currículo.",
};

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
  const [lastCollection, setLastCollection] =
    useState<EvidenceCollection | null>(null);

  if (!snapshotId) return null;

  const items = evidence.data ?? [];
  const confirmed = items.filter((item) => item.reviewState === "confirmado");
  const groups = groupBySubject(items);

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

        {groups.map((group) => (
          <section key={group.key} className="flex flex-col gap-2">
            <div className="flex items-center justify-between gap-2">
              <h4 className="text-xs font-semibold text-on-surface">
                {group.label}
              </h4>
              {group.pending > 0 && (
                <button
                  type="button"
                  disabled={review.isPending}
                  onClick={() => confirmGroup(group.items)}
                  className="rounded-lg bg-surface-container-high px-2 py-0.5 text-[11px] font-medium text-on-surface disabled:opacity-60"
                >
                  Confirmar {group.pending} pendente(s)
                </button>
              )}
            </div>
            {group.items.map((item) => (
              <EvidenceItem
                key={item.id}
                item={item}
                onReview={(reviewState) =>
                  review.mutate({ id: item.id, reviewState })
                }
                busy={review.isPending}
              />
            ))}
          </section>
        ))}
      </div>
    </div>
  );
}

const ORDER: Record<string, number> = {
  pendente: 0,
  confirmado: 1,
  descartado: 2,
};
const SOURCE: Record<string, string> = {
  base_link: "Link do projeto",
  web: "Busca web",
  vault: "Second brain",
};

/** Agrupa por item do currículo (projeto ou empresa), com o que falta decidir primeiro. */
function groupBySubject(items: SupplementaryEvidence[]) {
  const groups = new Map<
    string,
    {
      key: string;
      label: string;
      items: SupplementaryEvidence[];
      pending: number;
    }
  >();
  for (const item of items) {
    const key = item.subjectKey ?? "outros";
    const group = groups.get(key) ?? {
      key,
      label: item.subjectLabel ?? "Outros links",
      items: [],
      pending: 0,
    };
    group.items.push(item);
    if (item.reviewState === "pendente") group.pending++;
    groups.set(key, group);
  }
  return [...groups.values()]
    .map((group) => ({
      ...group,
      items: group.items.sort(
        (left, right) => ORDER[left.reviewState] - ORDER[right.reviewState],
      ),
    }))
    .sort(
      (left, right) => Number(right.pending > 0) - Number(left.pending > 0),
    );
}

function EvidenceItem({
  item,
  onReview,
  busy,
}: {
  item: SupplementaryEvidence;
  onReview: (reviewState: "pendente" | "confirmado" | "descartado") => void;
  busy: boolean;
}) {
  const confirmed = item.reviewState === "confirmado";
  const discarded = item.reviewState === "descartado";
  const label = {
    confirmado: "Confirmado",
    descartado: "Descartado",
    pendente: "Pendente",
  }[item.reviewState];

  return (
    <article className="flex flex-col gap-2 rounded-lg bg-surface-container p-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-on-surface">{item.claim}</p>
          <span className="text-[11px] text-on-surface-variant">
            {SOURCE[item.sourceKind] ?? item.sourceKind}
          </span>
        </div>
        <span
          className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
            confirmed
              ? "bg-secondary-container text-on-secondary-container"
              : "bg-surface-container-high text-on-surface-variant"
          }`}
        >
          {label}
        </span>
      </div>
      <blockquote className="border-l-2 border-outline-variant pl-2 text-xs text-on-surface-variant">
        “{item.sourceExcerpt}”
      </blockquote>
      <div className="flex items-center justify-between gap-3">
        {item.sourceUrl ? (
          <a
            href={item.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="truncate text-xs text-primary underline"
          >
            {item.sourceUrl}
          </a>
        ) : (
          <span />
        )}
        <div className="flex shrink-0 gap-2">
          {confirmed || discarded ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => onReview("pendente")}
              className="rounded-lg bg-surface-container-high px-2.5 py-1 text-xs font-medium text-on-surface disabled:opacity-60"
            >
              {discarded ? "Restaurar" : "Desfazer"}
            </button>
          ) : (
            <>
              <button
                type="button"
                disabled={busy}
                onClick={() => onReview("descartado")}
                className="rounded-lg bg-surface-container-high px-2.5 py-1 text-xs font-medium text-on-surface disabled:opacity-60"
              >
                Descartar
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => onReview("confirmado")}
                className="rounded-lg bg-primary-950 px-2.5 py-1 text-xs font-medium text-white disabled:opacity-60"
              >
                Confirmar
              </button>
            </>
          )}
        </div>
      </div>
    </article>
  );
}
