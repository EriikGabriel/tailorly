import {
  EvidenceReviewList,
  groupBySubject,
} from "@components/evidence/evidence-review-list";
import {
  type SupplementaryEvidence,
  useEvidenceSnapshot,
  useReviewSupplementaryEvidence,
  useSupplementaryEvidence,
} from "@queries/supplementary-evidence";

/**
 * Evidências complementares encontradas em links, na web e no second brain, dentro do diálogo de
 * revisão antes de gerar. Só as confirmadas entram no currículo; as pendentes ficam de fora.
 */
export function EvidenceSection() {
  const { snapshotId, token } = useEvidenceSnapshot();
  const evidence = useSupplementaryEvidence(snapshotId, token);
  const review = useReviewSupplementaryEvidence(snapshotId, token);

  const items = evidence.data ?? [];
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

  if (items.length === 0) return null;

  return (
    <section
      aria-labelledby="evidence-review-heading"
      className="flex flex-col gap-3"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/30 pb-1.5">
        <h3
          id="evidence-review-heading"
          className="text-xs font-bold uppercase tracking-wide text-secondary-700"
        >
          Evidências complementares
        </h3>
        <p className="text-xs text-on-surface-variant" role="status">
          {pendingCount > 0
            ? `${pendingCount} pendente(s): sem confirmação, ficam fora do currículo`
            : "Nenhuma pendente"}
        </p>
      </div>
      <EvidenceReviewList
        groups={groups}
        busy={review.isPending}
        onReview={(id, reviewState) => review.mutate({ id, reviewState })}
        onConfirmGroup={confirmGroup}
      />
    </section>
  );
}
