import { Button } from "@animate/buttons/button";
import {
  ArrowRight01Icon,
  Download04Icon,
  Edit02Icon,
  FileTextIcon,
  HistoryIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Link } from "@tanstack/react-router";

const recentCVs = [
  {
    title: "Senior Tech Lead - FinTech Solutions",
    match: "96% ATS Match",
    created: "Gerado há 2 horas",
    context: "Nubank LATAM Context",
    version: "Versão Executiva (1 Página)",
  },
  {
    title: "Staff Software Engineer - Cloud Global",
    match: "94% ATS Match",
    created: "Ontem, às 17:42",
    context: "Banco Inter Cloud Architecture",
    version: "Versão Técnica Detalhada",
  },
  {
    title: "Principal Architect - E-commerce Scale",
    match: "91% ATS Match",
    created: "3 dias atrás",
    context: "Mercado Livre Core Engine",
    version: "Versão Monográfica",
  },
] as const;

export function RecentCVSection() {
  return (
    <section
      aria-labelledby="recent-curricula-heading"
      className="w-full rounded-xl bg-surface-container-low px-5 pb-8 pt-8 shadow-[0_1px_1px_rgba(0,0,0,0.05)] sm:px-10 sm:pb-10 sm:pt-14"
    >
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 items-start gap-2">
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-container">
            <HugeiconsIcon
              icon={HistoryIcon}
              className="size-3.5 text-primary-950"
            />
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
          Ver todos (18)
          <HugeiconsIcon icon={ArrowRight01Icon} className="size-3" />
        </Link>
      </div>

      <ul className="space-y-1">
        {recentCVs.map((cv) => (
          <li
            key={cv.title}
            className="flex flex-col gap-3 rounded-lg bg-card p-4 shadow-[0_1px_1px_rgba(0,0,0,0.05)] sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex min-w-0 items-start gap-4 sm:items-center">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-surface-container-high">
                <HugeiconsIcon
                  icon={FileTextIcon}
                  className="size-4 text-primary-950"
                />
              </span>
              <div className="min-w-0 space-y-0.5">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="min-w-0 text-base font-semibold leading-6 text-primary-950">
                    {cv.title}
                  </h3>
                  <span className="rounded-full bg-secondary-container/40 px-2 py-0.5 text-xs font-semibold leading-4 tracking-[0.3px] text-secondary-800">
                    {cv.match}
                  </span>
                </div>
                <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface-variant">
                  <span>{cv.created}</span>
                  <span aria-hidden="true">•</span>
                  <span>{cv.context}</span>
                  <span aria-hidden="true">•</span>
                  <span>{cv.version}</span>
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-1 self-end sm:self-auto">
              <Link
                to="/generator"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-lg bg-surface-container px-4 py-1.5 text-xs font-semibold leading-4 tracking-[0.3px] text-primary-950 hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
              >
                <HugeiconsIcon
                  strokeWidth={2.5}
                  icon={Edit02Icon}
                  className="size-3"
                />
                Abrir Editor
              </Link>
              <Button
                type="button"
                variant="ghost"
                title="Download indisponível nesta prévia"
                aria-label={`Download de ${cv.title} indisponível nesta prévia`}
                className="flex size-8 items-center justify-center rounded-lg text-primary-950"
              >
                <HugeiconsIcon
                  icon={Download04Icon}
                  strokeWidth={1.5}
                  className="size-4.5"
                />
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
