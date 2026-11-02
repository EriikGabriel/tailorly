import { CvHistoryRow } from "@components/cv-history-list";
import { useCvHistory } from "@queries/cv-generation";
import { useMemo, useState } from "react";
import {
  applyFilters,
  type CvFilters,
  companiesOf,
  defaultFilters,
  statsOf,
} from "../-utils/cv-filters";
import { CvStatsRow } from "./cv-stats";
import { CvToolbar } from "./cv-toolbar";

/** Biblioteca de currículos gerados: resumo, busca, filtros, ordenação e ações por item. */
export function CvLibrary() {
  const history = useCvHistory();
  const [filters, setFilters] = useState<CvFilters>(defaultFilters);
  const [error, setError] = useState<string | null>(null);

  const all = history.data;
  const companies = useMemo(() => companiesOf(all ?? []), [all]);
  const stats = useMemo(() => statsOf(all ?? []), [all]);
  const visible = useMemo(
    () => applyFilters(all ?? [], filters),
    [all, filters],
  );

  if (history.isLoading) {
    return <p className="text-sm text-on-surface-variant">Carregando...</p>;
  }
  if (history.isError) {
    return (
      <p role="alert" className="text-sm text-error">
        Não foi possível carregar seus currículos.
      </p>
    );
  }
  if (!all || all.length === 0) {
    return (
      <p className="rounded-lg bg-card p-4 text-sm text-on-surface-variant shadow-xs">
        Você ainda não gerou nenhum currículo. Leia uma vaga e gere o primeiro a
        partir do seu Currículo Base aprovado.
      </p>
    );
  }

  // Se a empresa filtrada deixou de existir (ex.: currículo excluído), o filtro some.
  const effective =
    filters.company && !companies.includes(filters.company)
      ? { ...filters, company: "" }
      : filters;

  return (
    <div className="flex flex-col gap-4">
      <CvStatsRow stats={stats} />
      <CvToolbar
        filters={effective}
        companies={companies}
        onChange={(patch) => setFilters({ ...effective, ...patch })}
        onReset={() => setFilters(defaultFilters)}
      />

      <p aria-live="polite" className="text-sm text-on-surface-variant">
        {visible.length === all.length
          ? `${all.length} ${all.length === 1 ? "currículo" : "currículos"}`
          : `${visible.length} de ${all.length} currículos`}
      </p>

      {visible.length === 0 ? (
        <p className="rounded-lg bg-card p-4 text-sm text-on-surface-variant shadow-xs">
          Nenhum currículo corresponde à busca e aos filtros atuais.
        </p>
      ) : (
        <ul className="space-y-1">
          {visible.map((cv) => (
            <CvHistoryRow key={cv.id} cv={cv} onError={setError} />
          ))}
        </ul>
      )}
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}
    </div>
  );
}
