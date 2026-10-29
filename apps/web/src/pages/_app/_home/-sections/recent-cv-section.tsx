import { CvHistoryList } from "@components/cv-history-list";
import { ArrowRight, History } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

export function RecentCVSection() {
  return (
    <section
      aria-labelledby="recent-curricula-heading"
      className="w-full rounded-xl bg-surface-container-low px-5 pb-8 pt-8 shadow-[0_1px_1px_rgba(0,0,0,0.05)] sm:px-10 sm:pb-10 sm:pt-14"
    >
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 items-start gap-2">
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-container">
            <History className="size-3.5 text-primary-950" />
          </span>
          <div className="min-w-0">
            <h2
              id="recent-curricula-heading"
              className="text-base font-semibold leading-6 text-primary-950"
            >
              Últimos Currículos Gerados
            </h2>
            <p className="text-[13px] leading-5 text-on-surface-variant">
              Seus documentos otimizados prontos para reedição ou download
            </p>
          </div>
        </div>
        <Link
          to="/cvs"
          className="inline-flex shrink-0 items-center gap-1 self-start rounded-sm text-sm font-semibold leading-5 tracking-[0.14px] text-primary-950 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900 sm:mt-1"
        >
          Ver todos
          <ArrowRight className="size-3" />
        </Link>
      </div>

      <CvHistoryList limit={3} />
    </section>
  );
}
