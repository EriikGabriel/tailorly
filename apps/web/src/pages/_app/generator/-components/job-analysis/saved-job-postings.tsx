import { useJobPostingList } from "@queries/job-posting";
import { useAuthStore } from "@stores/auth-store";
import { useJobPostingStore } from "@stores/job-posting-store";

/** Lista de vagas salvas do owner (conta ou sessão anônima) para reabrir a análise. */
export function SavedJobPostings() {
  const list = useJobPostingList();
  const select = useJobPostingStore((state) => state.select);
  const accountToken = useAuthStore((state) => state.accessToken);
  const items = list.data ?? [];
  if (items.length === 0) return null;

  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
        Vagas salvas:
      </p>
      <ul className="flex flex-col gap-1">
        {items.map((item) => (
          <li key={item.id}>
            <button
              type="button"
              onClick={() =>
                item.id &&
                select(item.id, accountToken ? "account" : "anonymous")
              }
              className="flex w-full items-center justify-between gap-2 rounded-lg bg-surface-container px-3 py-2 text-left text-[13px] text-on-surface hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-ring"
            >
              <span className="truncate font-semibold">
                {item.title ?? "Vaga sem título"}
                {item.companyName ? ` · ${item.companyName}` : ""}
              </span>
              <span className="shrink-0 text-xs text-on-surface-variant">
                v{item.latestVersionNumber}
                {item.expired ? " · encerrada" : ""}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
