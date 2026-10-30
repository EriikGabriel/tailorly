import {
  useDeleteJobPosting,
  useJobPostingVersion,
  useReanalyzeJobPosting,
  useSelectedJobPosting,
} from "@queries/job-posting";
import { Building2, Trash2 } from "@react-zero-ui/icon-sprite";
import { useJobPostingStore } from "@stores/job-posting-store";
import { AnalysisStatus } from "./job-analysis/analysis-status";
import { BulletList } from "./job-analysis/bullet-list";
import { CollapsibleSection } from "./job-analysis/collapsible-section";
import { CompanyBlock } from "./job-analysis/company-block";
import { JobAnalysisFooter } from "./job-analysis/job-analysis-footer";
import { JobMetaChips } from "./job-analysis/job-meta-chips";
import { JobTechnologies } from "./job-analysis/job-technologies";
import { JobWarnings } from "./job-analysis/job-warnings";
import { RequirementsBlock } from "./job-analysis/requirements-block";
import { SavedJobPostings } from "./job-analysis/saved-job-postings";

/**
 * Análise da vaga selecionada: dados lidos da página, requisitos, tecnologias e seções. Sem vaga
 * selecionada, lista as vagas salvas.
 */
export function JobAnalysisCard() {
  const selected = useJobPostingStore((state) => state.selected);
  const clear = useJobPostingStore((state) => state.clear);
  const detail = useSelectedJobPosting();
  const { version } = useJobPostingVersion(detail.data);
  const reanalyze = useReanalyzeJobPosting();
  const remove = useDeleteJobPosting();

  if (!selected) return <SavedJobPostings />;

  if (detail.isError) {
    return (
      <div
        role="alert"
        className="flex items-center justify-between gap-2 rounded-lg bg-error-container p-3 text-[13px] text-on-error-container"
      >
        Não foi possível abrir a vaga salva.
        <button
          type="button"
          onClick={clear}
          className="font-semibold underline"
        >
          Fechar
        </button>
      </div>
    );
  }
  if (!detail.data || !version?.details) {
    return (
      <p role="status" className="text-[13px] text-on-surface-variant">
        Carregando vaga...
      </p>
    );
  }

  const details = version.details;
  const analysis = version.analysis;
  const responsibilities = details.responsibilities ?? [];
  const benefits = details.benefits ?? [];
  const hiringProcess = details.hiringProcess ?? [];

  return (
    <article
      aria-label="Análise da vaga"
      className="flex flex-col gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-3.75"
    >
      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h4 className="text-[15px] font-bold leading-5 text-on-surface">
            {details.title ?? "Vaga sem título"}
          </h4>
          <p className="flex items-center gap-1 text-[13px] text-on-surface-variant">
            <Building2 className="size-3.5" aria-hidden="true" />
            {details.company?.name ?? "Empresa não informada"}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            aria-label="Descartar vaga salva"
            title="Descartar vaga salva"
            disabled={remove.isPending}
            onClick={() =>
              detail.data.posting?.id && remove.mutate(detail.data.posting.id)
            }
            className="rounded-md p-1.5 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-ring disabled:opacity-50"
          >
            <Trash2 className="size-4" aria-hidden="true" />
          </button>
        </div>
      </header>

      <JobMetaChips details={details} analysis={analysis} />
      <JobWarnings warnings={details.warnings ?? []} />

      <AnalysisStatus
        version={version}
        retrying={reanalyze.isPending}
        onRetry={() =>
          version.jobPostingId &&
          version.versionNumber &&
          reanalyze.mutate({
            postingId: version.jobPostingId,
            versionNumber: version.versionNumber,
          })
        }
      />

      <RequirementsBlock details={details} analysis={analysis} />
      <JobTechnologies technologies={analysis?.technologies ?? []} />

      {responsibilities.length > 0 && (
        <CollapsibleSection
          title="Responsabilidades"
          count={responsibilities.length}
        >
          <BulletList items={responsibilities} />
        </CollapsibleSection>
      )}
      {benefits.length > 0 && (
        <CollapsibleSection title="Benefícios" count={benefits.length}>
          <BulletList items={benefits} />
        </CollapsibleSection>
      )}
      {hiringProcess.length > 0 && (
        <CollapsibleSection
          title="Processo seletivo"
          count={hiringProcess.length}
        >
          <BulletList items={hiringProcess} ordered />
        </CollapsibleSection>
      )}
      {details.company && (
        <CollapsibleSection title="Sobre a empresa">
          <CompanyBlock company={details.company} />
        </CollapsibleSection>
      )}

      <JobAnalysisFooter
        detail={detail.data}
        version={version}
        finalUrl={details.finalUrl}
        onShowSaved={clear}
      />
    </article>
  );
}
