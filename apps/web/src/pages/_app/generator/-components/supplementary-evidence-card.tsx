import { useCurrentAccount } from "@queries/current-account";
import { useViewer } from "@queries/cv-generation";
import {
  type EvidenceCollection,
  type SupplementaryEvidence,
  useCollectSupplementaryEvidence,
  useReviewSupplementaryEvidence,
  useSupplementaryEvidence,
} from "@queries/supplementary-evidence";
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
  const review = useReviewSupplementaryEvidence(snapshotId, viewer.token);
  const [lastCollection, setLastCollection] =
    useState<EvidenceCollection | null>(null);

  if (!snapshotId) return null;

  const items = evidence.data ?? [];
  const pending = items.filter((item) => item.reviewState === "pendente");
  const confirmed = items.filter((item) => item.reviewState === "confirmado");
  const discarded = items.filter((item) => item.reviewState === "descartado");

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
            disabled={collect.isPending}
            className="shrink-0 rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high disabled:opacity-60"
          >
            {collect.isPending ? "Analisando..." : "Analisar links"}
          </button>
        </div>

        {lastCollection && (
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
            Nenhuma evidência ainda. Use “Analisar links” para buscar detalhes
            dos seus projetos.
          </p>
        )}

        {pending.map((item) => (
          <EvidenceItem
            key={item.id}
            item={item}
            onReview={(reviewState) =>
              review.mutate({ id: item.id, reviewState })
            }
            busy={review.isPending}
          />
        ))}
        {confirmed.map((item) => (
          <EvidenceItem
            key={item.id}
            item={item}
            onReview={(reviewState) =>
              review.mutate({ id: item.id, reviewState })
            }
            busy={review.isPending}
          />
        ))}
        {discarded.map((item) => (
          <EvidenceItem
            key={item.id}
            item={item}
            onReview={(reviewState) =>
              review.mutate({ id: item.id, reviewState })
            }
            busy={review.isPending}
          />
        ))}
      </div>
    </div>
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
        <p className="text-sm font-medium text-on-surface">{item.claim}</p>
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
