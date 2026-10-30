import type {
  JobPostingDetailResponse,
  JobPostingVersionResponse,
} from "@lib/api/generated/model";
import { formatDate } from "@/utils/date-format";
import { VersionPicker } from "./version-picker";

/** Rodapé do cartão: versão exibida, datas de leitura/conferência, link da página e volta à lista. */
export function JobAnalysisFooter({
  detail,
  version,
  finalUrl,
  onShowSaved,
}: {
  detail: JobPostingDetailResponse;
  version: JobPostingVersionResponse;
  finalUrl: string | undefined;
  onShowSaved: () => void;
}) {
  const readAt = formatDate(version.createdAt, true);
  const checkedAt = formatDate(version.lastCheckedAt, true);

  return (
    <footer className="flex flex-col gap-1.5 border-t border-outline-variant/60 pt-2 text-xs text-on-surface-variant">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <VersionPicker detail={detail} />
        <span>
          Versão {version.versionNumber}
          {readAt ? ` · lida em ${readAt}` : ""}
          {checkedAt && checkedAt !== readAt
            ? ` · conferida em ${checkedAt}`
            : ""}
        </span>
      </div>
      {finalUrl && (
        <a
          href={finalUrl}
          target="_blank"
          rel="noreferrer noopener"
          className="truncate text-secondary-700 hover:underline"
        >
          {finalUrl}
        </a>
      )}
      <button
        type="button"
        onClick={onShowSaved}
        className="self-start font-semibold text-secondary-700 hover:underline"
      >
        Ver vagas salvas
      </button>
    </footer>
  );
}
