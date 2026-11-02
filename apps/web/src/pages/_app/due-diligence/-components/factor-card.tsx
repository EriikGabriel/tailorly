import type {
  DiligenceFactorResult,
  DiligenceFinding,
} from "@lib/api/due-diligence";
import {
  CalendarDays,
  CircleCheck,
  CircleDashed,
  ExternalLink,
  Quote,
  TriangleAlert,
} from "@react-zero-ui/icon-sprite";
import {
  type DiligenceFactorMeta,
  factorStatusLabel,
  polarityLabel,
  sourceTypeLabel,
  uncertaintyLabel,
} from "@/constants/due-diligence";
import { formatDate } from "@/utils/date-format";

const statusStyle = {
  evidence_found: {
    icon: CircleCheck,
    tone: "bg-secondary-100 text-primary-950",
  },
  no_data: {
    icon: CircleDashed,
    tone: "bg-surface-container text-on-surface-variant",
  },
  source_unavailable: {
    icon: TriangleAlert,
    tone: "bg-tertiary-100 text-tertiary-700",
  },
} as const;

const uncertaintyTone = {
  low: "bg-[hsl(140_45%_91%)] text-[hsl(140_60%_26%)]",
  medium: "bg-tertiary-100 text-tertiary-700",
  high: "bg-error-container text-on-error-container",
} as const;

function Finding({ finding }: { finding: DiligenceFinding }) {
  return (
    <li className="space-y-2 rounded-lg bg-surface-container-low p-3">
      <p className="text-sm text-primary-950">{finding.claim}</p>
      <blockquote className="flex gap-2 border-l-2 border-outline-variant pl-2 text-xs italic text-on-surface-variant">
        <Quote aria-hidden="true" className="mt-0.5 size-3 shrink-0" />
        <span>{finding.excerpt}</span>
      </blockquote>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-on-surface-variant">
        <a
          href={finding.sourceUrl}
          target="_blank"
          rel="noopener noreferrer nofollow"
          className="inline-flex max-w-full items-center gap-1 font-semibold text-primary-950 underline-offset-2 hover:underline"
        >
          <ExternalLink aria-hidden="true" className="size-3 shrink-0" />
          <span className="truncate">
            {finding.sourceTitle || finding.sourceUrl}
          </span>
        </a>
        <span className="rounded-full bg-card px-2 py-0.5">
          {sourceTypeLabel[finding.sourceType]}
        </span>
        <span className="rounded-full bg-card px-2 py-0.5">
          {polarityLabel[finding.polarity]}
        </span>
        <span className="inline-flex items-center gap-1">
          <CalendarDays aria-hidden="true" className="size-3" />
          {formatDate(finding.retrievedAt)}
        </span>
      </div>
      {finding.unverifiedReport && (
        <p className="flex items-center gap-1 text-xs font-semibold text-on-surface-variant">
          <TriangleAlert aria-hidden="true" className="size-3" />
          Relato de pessoas: não comprova o fato.
        </p>
      )}
      {!finding.entityConfirmed && (
        <p className="flex items-center gap-1 text-xs font-semibold text-error">
          <TriangleAlert aria-hidden="true" className="size-3" />
          Não confirmado que a fonte trata desta empresa (possível homônimo).
        </p>
      )}
    </li>
  );
}

interface FactorCardProps {
  meta: DiligenceFactorMeta;
  result: DiligenceFactorResult | undefined;
}

export function FactorCard({ meta, result }: FactorCardProps) {
  const status = result?.status ?? "no_data";
  const { icon: StatusIcon, tone } = statusStyle[status];
  const FactorIcon = meta.icon;

  return (
    <section className="flex h-full flex-col gap-3 rounded-xl bg-card p-4 shadow-xs">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-surface-container text-primary-950">
          <FactorIcon aria-hidden="true" className="size-5" />
        </span>
        <div className="min-w-0 flex-1">
          <h4 className="text-base font-semibold leading-6 text-primary-950">
            {meta.label}
          </h4>
          <p className="text-xs text-on-surface-variant">{meta.description}</p>
        </div>
        <span
          className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${tone}`}
        >
          <StatusIcon aria-hidden="true" className="size-3.5" />
          {factorStatusLabel[status]}
        </span>
      </div>

      {status === "evidence_found" && result ? (
        <>
          {result.summary && (
            <p className="text-sm text-on-surface">{result.summary}</p>
          )}
          <ul className="space-y-2">
            {result.findings.map((finding) => (
              <Finding
                key={`${finding.sourceUrl}-${finding.excerpt}`}
                finding={finding}
              />
            ))}
          </ul>
          <p className="mt-auto flex flex-wrap items-center gap-2 text-xs text-on-surface-variant">
            <span
              className={`rounded-full px-2 py-0.5 font-semibold ${uncertaintyTone[result.uncertainty.level]}`}
            >
              {uncertaintyLabel[result.uncertainty.level]}
            </span>
            {result.uncertainty.note}
          </p>
        </>
      ) : status === "source_unavailable" ? (
        <p className="text-sm text-on-surface-variant">
          A busca deste fator falhou. Isso não indica nada sobre a empresa;
          refaça a análise mais tarde.
        </p>
      ) : (
        <p className="text-sm text-on-surface-variant">
          Nenhuma fonte pública relevante foi encontrada. A ausência de dados
          não indica problema nem comprova o contrário; informações privadas
          dependem de integração autorizada.
        </p>
      )}
    </section>
  );
}
