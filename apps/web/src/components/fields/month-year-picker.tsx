import { Popover, PopoverPanel, PopoverTrigger } from "@animate/base/popover";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  X,
} from "@react-zero-ui/icon-sprite";
import { useState } from "react";
import {
  formatPartialDate,
  monthShortName,
  parsePartialDate,
  toPartialValue,
} from "@/lib/review-format";
import { controlClass, iconClass } from "./field-styles";

interface MonthYearPickerProps {
  id: string;
  label: string;
  /** "YYYY-MM" (mês e ano) ou "YYYY" (só o ano). */
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  invalid?: boolean;
  describedBy?: string;
  minYear?: number;
  /** Último ano selecionável; o padrão é o ano atual. */
  maxYear?: number;
  /** Permite meses futuros do ano atual (ex.: validade de certificação). */
  allowFuture?: boolean;
  /** Ano exibido ao abrir quando ainda não há valor (ex.: o ano do início, no término). */
  initialYear?: number;
}

const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);

const stepClass =
  "flex size-8 items-center justify-center rounded-lg border border-outline-variant/50 bg-card text-primary-900 transition-colors hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30 focus-visible:outline-2 focus-visible:outline-primary-900";

const monthClass =
  "rounded-lg border border-transparent bg-surface-container-low px-1 py-2 text-center text-xs font-medium text-primary-950 capitalize transition-colors enabled:hover:bg-surface-container aria-pressed:bg-primary-900 aria-pressed:text-primary-foreground disabled:cursor-not-allowed disabled:opacity-30 focus-visible:outline-2 focus-visible:outline-primary-900";

export function MonthYearPicker({
  id,
  label,
  value,
  onChange,
  placeholder = "Selecionar data",
  disabled,
  invalid,
  describedBy,
  minYear = 1950,
  maxYear,
  allowFuture = false,
  initialYear,
}: MonthYearPickerProps) {
  const now = new Date();
  const thisYear = now.getFullYear();
  const lastYear = maxYear ?? thisYear;
  const parsed = parsePartialDate(value);
  const unrecognized = value.trim() !== "" && parsed === null;
  const display = value.trim() === "" ? null : formatPartialDate(value);

  const clamp = (year: number) => Math.min(lastYear, Math.max(minYear, year));
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(() =>
    clamp(parsed?.year ?? initialYear ?? thisYear),
  );

  function handleOpenChange(next: boolean) {
    if (next) setViewYear(clamp(parsed?.year ?? initialYear ?? thisYear));
    setOpen(next);
  }

  function pick(next: string) {
    onChange(next);
    setOpen(false);
  }

  const futureMonth = (month: number) =>
    !allowFuture && viewYear === thisYear && month > now.getMonth() + 1;

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <div
        className={`${controlClass} gap-1 pr-1`}
        aria-invalid={invalid || unrecognized || undefined}
        data-disabled={disabled ? "" : undefined}
      >
        <PopoverTrigger
          id={id}
          disabled={disabled}
          aria-label={`${label}: ${display ?? "não informado"}`}
          aria-describedby={describedBy}
          className="flex min-h-9 min-w-0 flex-1 items-center gap-2 text-left outline-none disabled:cursor-not-allowed"
        >
          <CalendarDays aria-hidden="true" className={iconClass} />
          <span
            className={`truncate ${display ? "" : "font-normal text-outline"}`}
          >
            {display ?? placeholder}
          </span>
        </PopoverTrigger>
        {display && !disabled && (
          <button
            type="button"
            aria-label={`Limpar ${label}`}
            onClick={() => onChange("")}
            className="rounded-md p-1 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
          >
            <X aria-hidden="true" className="size-3.5" />
          </button>
        )}
      </div>
      <PopoverPanel
        align="start"
        sideOffset={6}
        className="w-72 max-w-[calc(100vw-2rem)] p-0"
      >
        <div className="flex items-center justify-between gap-2 rounded-t-md border-b border-outline-variant/40 bg-surface-container-low px-3 py-2.5">
          <button
            type="button"
            aria-label="Ano anterior"
            className={stepClass}
            disabled={viewYear <= minYear}
            onClick={() => setViewYear((year) => clamp(year - 1))}
          >
            <ChevronLeft aria-hidden="true" className="size-4" />
          </button>
          <div className="text-center">
            <p className="text-sm font-semibold tabular-nums">{viewYear}</p>
            <p className="text-[11px] text-on-surface-variant">{label}</p>
          </div>
          <button
            type="button"
            aria-label="Próximo ano"
            className={stepClass}
            disabled={viewYear >= lastYear}
            onClick={() => setViewYear((year) => clamp(year + 1))}
          >
            <ChevronRight aria-hidden="true" className="size-4" />
          </button>
        </div>
        <div className="grid grid-cols-3 gap-1.5 p-3">
          {MONTHS.map((month) => (
            <button
              key={month}
              type="button"
              aria-pressed={parsed?.year === viewYear && parsed.month === month}
              disabled={futureMonth(month)}
              onClick={() => pick(toPartialValue(viewYear, month))}
              className={monthClass}
            >
              {monthShortName(month)}
            </button>
          ))}
        </div>
        <div className="flex items-center justify-between gap-2 border-t border-outline-variant/40 px-3 py-2">
          <button
            type="button"
            onClick={() => pick(toPartialValue(viewYear, null))}
            className="rounded-md px-2 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
          >
            Somente o ano {viewYear}
          </button>
          {unrecognized && (
            <span className="text-[11px] text-on-surface-variant">
              Valor do documento: {value}
            </span>
          )}
        </div>
      </PopoverPanel>
    </Popover>
  );
}
