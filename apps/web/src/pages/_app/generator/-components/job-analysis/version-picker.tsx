import type { JobPostingDetailResponse } from "@lib/api/generated/model";
import { useJobPostingStore } from "@stores/job-posting-store";
import { formatDate } from "@/utils/date-format";

/** Seletor de versão da vaga; só aparece quando há mais de uma versão salva. */
export function VersionPicker({
  detail,
}: {
  detail: JobPostingDetailResponse;
}) {
  const selected = useJobPostingStore((state) => state.selected);
  const setVersion = useJobPostingStore((state) => state.setVersion);
  const versions = detail.versions ?? [];
  const latest = detail.latest?.versionNumber ?? 1;
  if (versions.length < 2) return null;

  return (
    <label className="flex items-center gap-2 text-xs font-medium text-on-surface-variant">
      Versão
      <select
        value={selected?.versionNumber ?? latest}
        onChange={(event) => {
          const value = Number(event.target.value);
          setVersion(value === latest ? null : value);
        }}
        className="rounded-md bg-surface-container px-2 py-1 text-xs text-on-surface"
      >
        {versions.map((item) => (
          <option key={item.versionNumber} value={item.versionNumber}>
            v{item.versionNumber} · {formatDate(item.createdAt) ?? ""}
            {item.versionNumber === latest ? " (atual)" : ""}
          </option>
        ))}
      </select>
    </label>
  );
}
