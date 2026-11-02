import { apiErrorMessage } from "@lib/api/error-message";
import { useDueDiligence } from "@queries/due-diligence";
import {
  Building2,
  CalendarDays,
  CircleCheck,
  CircleDashed,
  Link,
  Lock,
  ScanSearch,
  TriangleAlert,
} from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";
import { diligenceFactors, diligenceGroups } from "@/constants/due-diligence";
import { FactorCard } from "./factor-card";

const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "long",
  timeStyle: "short",
});

function Stat({
  icon: Icon,
  label,
  value,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  label: string;
  value: number;
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-surface-container-low px-3 py-2">
      <Icon aria-hidden className="size-4 shrink-0 text-primary-950" />
      <div>
        <dd className="text-lg font-semibold leading-6 text-primary-950">
          {value}
        </dd>
        <dt className="text-xs text-on-surface-variant">{label}</dt>
      </div>
    </div>
  );
}

/** Relatório salvo: entidade, data da consulta, resumo e os onze fatores agrupados, sempre todos. */
export function DueDiligenceReportView({ id }: { id: string | null }) {
  const query = useDueDiligence(id);

  if (!id) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-xl bg-card p-10 text-center shadow-xs">
        <span className="flex size-12 items-center justify-center rounded-full bg-surface-container text-primary-950">
          <ScanSearch aria-hidden="true" className="size-6" />
        </span>
        <p className="max-w-md text-sm text-on-surface-variant">
          Peça uma análise ou escolha uma das suas análises salvas para ver os
          onze fatores, com a fonte, a data e a incerteza de cada conclusão.
        </p>
      </div>
    );
  }
  if (query.isLoading)
    return <p className="text-sm text-on-surface-variant">Carregando...</p>;
  if (query.isError || !query.data) {
    return (
      <p role="alert" className="text-sm text-error">
        {apiErrorMessage(query.error, "Não foi possível abrir a análise.")}
      </p>
    );
  }

  const { report, status } = query.data;
  if (status === "running" || !report) {
    return (
      <p
        role={status === "failed" ? "alert" : "status"}
        className="flex items-center gap-3 rounded-xl bg-card p-6 text-sm text-on-surface-variant shadow-xs"
      >
        {status === "failed" ? (
          <TriangleAlert aria-hidden="true" className="size-5 shrink-0" />
        ) : (
          <ScanSearch aria-hidden="true" className="size-5 shrink-0" />
        )}
        {status === "running"
          ? `Consultando fontes públicas sobre ${query.data.companyName}. O relatório aparece aqui quando terminar.`
          : `A análise automática de ${query.data.companyName} não pôde ser concluída. Isso não indica nada sobre a empresa; peça uma nova análise.`}
      </p>
    );
  }

  const byId = new Map(report.factors.map((factor) => [factor.factor, factor]));
  const count = (wanted: string) =>
    report.factors.filter((factor) => factor.status === wanted).length;
  const sources = new Set(
    report.factors.flatMap((factor) =>
      factor.findings.map((finding) => finding.sourceUrl),
    ),
  ).size;

  return (
    <article className="flex flex-col gap-6">
      <header className="flex flex-col gap-4 rounded-xl bg-card p-4 shadow-xs">
        <div className="flex items-start gap-3">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-surface-container text-primary-950">
            <Building2 aria-hidden="true" className="size-6" />
          </span>
          <div className="min-w-0">
            <h3 className="text-xl font-semibold text-primary-950">
              {report.entity.name}
            </h3>
            <p className="text-sm text-on-surface-variant">
              Entidade analisada
              {report.entity.website ? ` · ${report.entity.website}` : ""}
              {report.entity.jobTitle
                ? ` · vaga "${report.entity.jobTitle}" v${report.entity.jobVersionNumber}`
                : ""}
            </p>
          </div>
        </div>

        <dl className="grid grid-cols-2 gap-2 lg:grid-cols-4">
          <Stat
            icon={CircleCheck}
            label="Fatores com fontes"
            value={count("evidence_found")}
          />
          <Stat
            icon={CircleDashed}
            label="Sem dados públicos"
            value={count("no_data")}
          />
          <Stat
            icon={TriangleAlert}
            label="Busca indisponível"
            value={count("source_unavailable")}
          />
          <Stat icon={Link} label="Fontes distintas" value={sources} />
        </dl>

        <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-on-surface-variant">
          <span className="inline-flex items-center gap-1">
            <CalendarDays aria-hidden="true" className="size-3.5" />
            Consultado em {dateFormat.format(new Date(report.consultedAt))}
          </span>
          <span className="inline-flex items-center gap-1">
            <Lock aria-hidden="true" className="size-3.5" />
            {report.privateSourcesConsulted
              ? "Fontes públicas e privadas autorizadas"
              : "Só fontes públicas · nenhuma fonte privada consultada"}
          </span>
          <span>{report.analyzerVersion}</span>
        </p>
      </header>

      {diligenceGroups.map((group) => (
        <section
          key={group.id}
          aria-labelledby={`dd-group-${group.id}`}
          className="flex flex-col gap-3"
        >
          <div>
            <h3
              id={`dd-group-${group.id}`}
              className="text-base font-semibold text-primary-950"
            >
              {group.label}
            </h3>
            <p className="text-xs text-on-surface-variant">
              {group.description}
            </p>
          </div>
          {/* Com número ímpar de fatores, o último card ocupa as duas colunas para não deixar lacuna. */}
          {(() => {
            const items = diligenceFactors.filter(
              (meta) => meta.group === group.id,
            );
            const oddLast = items.length % 2 === 1;
            return (
              <ul className="grid gap-3 2xl:grid-cols-2">
                {items.map((meta, index) => (
                  <li
                    key={meta.id}
                    className={
                      oddLast && index === items.length - 1
                        ? "2xl:col-span-2"
                        : undefined
                    }
                  >
                    <FactorCard meta={meta} result={byId.get(meta.id)} />
                  </li>
                ))}
              </ul>
            );
          })()}
        </section>
      ))}
    </article>
  );
}
