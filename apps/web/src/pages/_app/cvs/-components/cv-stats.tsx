import { atsScoreColor } from "@/utils/ats-score";
import type { CvStats } from "../-utils/cv-filters";

/** Resumo do histórico completo (não muda com os filtros). */
export function CvStatsRow({ stats }: { stats: CvStats }) {
  const items = [
    { label: "Currículos gerados", value: String(stats.total) },
    { label: "Empresas", value: String(stats.companies) },
    {
      label: "Nota ATS média",
      value: stats.averageScore === null ? "—" : `${stats.averageScore}/100`,
      color:
        stats.averageScore === null
          ? undefined
          : atsScoreColor(stats.averageScore),
    },
    {
      label: "Melhor nota ATS",
      value: stats.bestScore === null ? "—" : `${stats.bestScore}/100`,
      color:
        stats.bestScore === null ? undefined : atsScoreColor(stats.bestScore),
    },
    { label: "Na meta ATS", value: String(stats.meetingTarget) },
  ];

  return (
    <dl className="grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-5">
      {items.map((item) => (
        <div
          key={item.label}
          className="rounded-lg bg-card px-4 py-3 shadow-xs"
        >
          <dt className="text-xs font-semibold tracking-[0.3px] text-on-surface-variant">
            {item.label}
          </dt>
          <dd
            className="text-xl font-semibold leading-7 text-primary-950"
            style={item.color ? { color: item.color } : undefined}
          >
            {item.value}
          </dd>
        </div>
      ))}
    </dl>
  );
}
