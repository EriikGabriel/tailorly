import type { PeriodConfig } from "@@types/review-model";
import { Switch } from "@animate/base/switch";
import { useId } from "react";
import {
  durationLabel,
  isCurrentStatus,
  parsePartialDate,
} from "@/lib/review-format";
import { FieldFrame } from "./field-frame";
import { MonthYearPicker } from "./month-year-picker";

type Values = Record<string, string>;

interface PeriodFieldProps {
  id: string;
  period: PeriodConfig;
  values: Values;
  onChange: (values: Values) => void;
}

const ENDED = /^(encerrado|conclu[ií]do|finalizado|completo)/i;

// A situação só é reescrita quando é vazia ou um dos valores que este campo gerencia;
// um texto próprio do documento (ex.: "Interrompido") nunca é sobrescrito.
function isManagedStatus(status: string): boolean {
  return status.trim() === "" || isCurrentStatus(status) || ENDED.test(status);
}

function ordinal(value: string, end: boolean): number | null {
  const date = parsePartialDate(value);
  if (!date) return null;
  return date.year * 12 + (date.month ?? (end ? 12 : 1));
}

export function PeriodField({
  id,
  period,
  values,
  onChange,
}: PeriodFieldProps) {
  const switchId = useId();
  const start = values[period.startKey] ?? "";
  const end = values[period.endKey] ?? "";
  const status = period.statusKey ? (values[period.statusKey] ?? "") : "";
  const hasSwitch = Boolean(period.currentLabel && period.statusKey);
  const current = hasSwitch && isCurrentStatus(status);

  const from = ordinal(start, false);
  const to = ordinal(end, true);
  const reversed = from !== null && to !== null && to < from;
  const duration = durationLabel(start, end, current);

  function update(patch: Values) {
    onChange({ ...values, ...patch });
  }

  function setEnd(value: string) {
    const patch: Values = { [period.endKey]: value };
    if (period.statusKey && value && isManagedStatus(status)) {
      patch[period.statusKey] = period.endedStatus ?? "";
    }
    update(patch);
  }

  function setCurrent(next: boolean) {
    if (!period.statusKey) return;
    if (next) {
      update({
        [period.endKey]: "",
        [period.statusKey]: period.currentStatus ?? "",
      });
      return;
    }
    update({
      [period.statusKey]:
        end && isManagedStatus(status) ? (period.endedStatus ?? "") : "",
    });
  }

  return (
    <div className="grid min-w-0 gap-3 sm:col-span-2 sm:grid-cols-2">
      <FieldFrame id={`${id}-start`} label={period.startLabel}>
        <MonthYearPicker
          id={`${id}-start`}
          label={period.startLabel}
          value={start}
          onChange={(value) => update({ [period.startKey]: value })}
        />
      </FieldFrame>
      <FieldFrame id={`${id}-end`} label={period.endLabel}>
        <MonthYearPicker
          id={`${id}-end`}
          label={period.endLabel}
          value={end}
          onChange={setEnd}
          disabled={current}
          invalid={reversed}
          describedBy={reversed ? `${id}-order` : undefined}
          placeholder={current ? "Em curso" : "Selecionar data"}
          allowFuture={period.futureEnd}
          initialYear={parsePartialDate(start)?.year}
          maxYear={period.futureEnd ? new Date().getFullYear() + 30 : undefined}
        />
      </FieldFrame>
      {(hasSwitch || duration || reversed) && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-surface-container-low px-3 py-2 sm:col-span-2">
          {hasSwitch ? (
            <div className="flex items-center gap-2">
              <Switch
                id={switchId}
                checked={current}
                onCheckedChange={setCurrent}
                aria-label={period.currentLabel}
              />
              <label htmlFor={switchId} className="text-sm font-medium">
                {period.currentLabel}
              </label>
            </div>
          ) : (
            <span />
          )}
          {reversed ? (
            <p
              id={`${id}-order`}
              role="alert"
              className="text-xs font-semibold text-destructive"
            >
              O término é anterior ao início.
            </p>
          ) : (
            duration && (
              <span className="rounded-full bg-secondary-container px-2.5 py-0.5 text-xs font-semibold text-on-secondary-container">
                {duration}
              </span>
            )
          )}
        </div>
      )}
    </div>
  );
}
