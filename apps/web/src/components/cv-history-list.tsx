import { Button } from "@animate/buttons/button";
import { apiErrorMessage } from "@lib/api/error-message";
import {
  downloadCvFile,
  useCvHistory,
  useDeleteCvGeneration,
  useViewer,
} from "@queries/cv-generation";
import { Download, FileText, SquarePen, X } from "@react-zero-ui/icon-sprite";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { Link } from "@tanstack/react-router";
import { useState } from "react";

const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

/** Histórico real de currículos gerados do owner atual, com abertura, download e exclusão. */
export function CvHistoryList({ limit }: { limit?: number }) {
  const history = useCvHistory();
  const viewer = useViewer();
  const remove = useDeleteCvGeneration();
  const [error, setError] = useState<string | null>(null);

  if (history.isLoading) {
    return <p className="text-sm text-on-surface-variant">Carregando...</p>;
  }
  const items = (history.data ?? []).slice(0, limit);
  if (items.length === 0) {
    return (
      <p className="rounded-lg bg-card p-4 text-sm text-on-surface-variant shadow-xs">
        Você ainda não gerou nenhum currículo. Leia uma vaga e gere o primeiro a
        partir do seu Currículo Base aprovado.
      </p>
    );
  }

  async function download(id: string) {
    if (!viewer.token) return;
    setError(null);
    try {
      await downloadCvFile(id, "pdf", viewer.token);
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível baixar o arquivo."));
    }
  }

  return (
    <>
      <ul className="space-y-1">
        {items.map((cv) => (
          <li
            key={cv.id}
            className="flex flex-col gap-3 rounded-lg bg-card p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex min-w-0 items-start gap-4 sm:items-center">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-surface-container-high">
                <FileText
                  className="size-4 text-primary-950"
                  aria-hidden="true"
                />
              </span>
              <div className="min-w-0 space-y-0.5">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="min-w-0 text-base font-semibold leading-6 text-primary-950">
                    {cv.job?.title ?? "Vaga sem título"}
                    {cv.job?.companyName ? ` — ${cv.job.companyName}` : ""}
                  </h3>
                  {cv.ats?.report?.score !== undefined && (
                    <span className="rounded-full bg-secondary-container/40 px-2 py-0.5 text-xs font-semibold text-secondary-800">
                      ATS {cv.ats.report.score}/100
                    </span>
                  )}
                </div>
                <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs font-semibold tracking-[0.3px] text-on-surface-variant">
                  <span>
                    {cv.createdAt
                      ? dateFormat.format(new Date(cv.createdAt))
                      : ""}
                  </span>
                  <span aria-hidden="true">•</span>
                  <span>Ground Truth v{cv.groundTruth?.snapshotNumber}</span>
                  <span aria-hidden="true">•</span>
                  <span>Vaga v{cv.job?.versionNumber}</span>
                  <span aria-hidden="true">•</span>
                  <span>
                    Template {cv.template?.name} v{cv.template?.version}
                  </span>
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-1 self-end sm:self-auto">
              <Link
                to="/generator"
                onClick={() =>
                  cv.id &&
                  viewer.kind &&
                  useCvGenerationStore.getState().select(cv.id, viewer.kind)
                }
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-lg bg-surface-container px-4 py-1.5 text-xs font-semibold tracking-[0.3px] text-primary-950 hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
              >
                <SquarePen
                  strokeWidth={2.5}
                  className="size-3"
                  aria-hidden="true"
                />
                Abrir Editor
              </Link>
              <Button
                type="button"
                variant="ghost"
                aria-label="Baixar PDF"
                onClick={() => cv.id && download(cv.id)}
                className="flex size-8 items-center justify-center rounded-lg text-primary-950"
              >
                <Download strokeWidth={1.5} className="size-4.5" />
              </Button>
              <Button
                type="button"
                variant="ghost"
                aria-label="Excluir currículo e arquivos"
                disabled={remove.isPending}
                onClick={() =>
                  cv.id &&
                  remove.mutate(cv.id, {
                    onError: (cause) =>
                      setError(
                        apiErrorMessage(cause, "Não foi possível excluir."),
                      ),
                  })
                }
                className="flex size-8 items-center justify-center rounded-lg text-primary-950"
              >
                <X strokeWidth={1.5} className="size-4" />
              </Button>
            </div>
          </li>
        ))}
      </ul>
      {error && (
        <p role="alert" className="mt-2 text-sm text-error">
          {error}
        </p>
      )}
    </>
  );
}
