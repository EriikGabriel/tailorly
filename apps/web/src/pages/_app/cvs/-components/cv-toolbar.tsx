import { Button } from "@animate/buttons/button";
import { IconInput } from "@components/fields/icon-input";
import {
  SelectField,
  type SelectOption,
} from "@components/fields/select-field";
import {
  ArrowUpDown,
  Building2,
  CalendarDays,
  FilterX,
  Gauge,
  type IconProps,
  Search,
} from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";
import {
  type AtsBand,
  atsBandOptions,
  type CvFilters,
  hasActiveFilters,
  type PeriodFilter,
  periodOptions,
  type SortKey,
  sortOptions,
} from "../-utils/cv-filters";

/** Rótulo visível acima do campo; o campo em si é o `SelectField` compartilhado (Base UI, acessível). */
function FilterSelect({
  id,
  label,
  icon,
  value,
  onChange,
  options,
  placeholder,
}: {
  id: string;
  label: string;
  icon: ComponentType<IconProps>;
  value: string;
  onChange: (value: string) => void;
  options: readonly SelectOption[];
  /** Com placeholder, a opção vazia vira o item "sem filtro" (ex.: todas as empresas). */
  placeholder?: string;
}) {
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <label
        htmlFor={id}
        className="text-xs font-semibold tracking-[0.3px] text-on-surface-variant"
      >
        {label}
      </label>
      <SelectField
        id={id}
        icon={icon}
        value={value}
        onChange={onChange}
        options={options}
        placeholder={placeholder}
        emptyOption={placeholder !== undefined}
      />
    </div>
  );
}

interface CvToolbarProps {
  filters: CvFilters;
  companies: string[];
  onChange: (patch: Partial<CvFilters>) => void;
  onReset: () => void;
}

/** Busca, filtros (vaga/empresa, nota ATS, data) e ordenação do histórico. */
export function CvToolbar({
  filters,
  companies,
  onChange,
  onReset,
}: CvToolbarProps) {
  const companyOptions = companies.map((name) => ({
    value: name,
    label: name,
  }));

  return (
    <search className="flex flex-col gap-3 rounded-xl bg-card p-4 shadow-xs">
      <div className="flex flex-col gap-1">
        <label
          htmlFor="cv-search"
          className="text-xs font-semibold tracking-[0.3px] text-on-surface-variant"
        >
          Pesquisar por vaga, empresa, template ou termo da vaga
        </label>
        <IconInput
          id="cv-search"
          icon={Search}
          value={filters.query}
          onChange={(query) => onChange({ query })}
          placeholder="Ex.: engenheiro backend, Nubank, Java…"
        />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <FilterSelect
          id="cv-company"
          label="Vaga / empresa"
          icon={Building2}
          value={filters.company}
          onChange={(company) => onChange({ company })}
          options={companyOptions}
          placeholder="Todas as empresas"
        />
        <FilterSelect
          id="cv-ats"
          label="Score ATS"
          icon={Gauge}
          value={filters.ats}
          onChange={(ats) => onChange({ ats: ats as AtsBand })}
          options={atsBandOptions}
        />
        <FilterSelect
          id="cv-period"
          label="Data de geração"
          icon={CalendarDays}
          value={filters.period}
          onChange={(period) => onChange({ period: period as PeriodFilter })}
          options={periodOptions}
        />
        <FilterSelect
          id="cv-sort"
          label="Ordenar por"
          icon={ArrowUpDown}
          value={filters.sort}
          onChange={(sort) => onChange({ sort: sort as SortKey })}
          options={sortOptions}
        />
      </div>

      {hasActiveFilters(filters) && (
        <div className="flex justify-end">
          <Button type="button" variant="ghost" size="sm" onClick={onReset}>
            <FilterX aria-hidden="true" className="size-4" />
            Limpar filtros
          </Button>
        </div>
      )}
    </search>
  );
}
