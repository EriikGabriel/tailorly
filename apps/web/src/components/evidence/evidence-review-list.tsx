import type { SupplementaryEvidence } from "@queries/supplementary-evidence";

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
export function groupBySubject(items: SupplementaryEvidence[]) {
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

export function EvidenceItem({
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

/** Evidências agrupadas por projeto ou empresa, para decidir uma a uma ou confirmar o grupo todo. */
export function EvidenceReviewList({
  groups,
  busy,
  onReview,
  onConfirmGroup,
}: {
  groups: ReturnType<typeof groupBySubject>;
  busy: boolean;
  onReview: (
    id: string,
    reviewState: "pendente" | "confirmado" | "descartado",
  ) => void;
  onConfirmGroup: (items: SupplementaryEvidence[]) => void;
}) {
  return (
    <div className="flex flex-col gap-5">
      {groups.map((group) => (
        <section key={group.key} className="flex flex-col gap-2">
          <div className="flex items-center justify-between gap-2">
            <h4 className="text-sm font-semibold text-on-surface">
              {group.label}
            </h4>
            {group.pending > 0 && (
              <button
                type="button"
                disabled={busy}
                onClick={() => onConfirmGroup(group.items)}
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
              onReview={(reviewState) => onReview(item.id, reviewState)}
              busy={busy}
            />
          ))}
        </section>
      ))}
    </div>
  );
}
