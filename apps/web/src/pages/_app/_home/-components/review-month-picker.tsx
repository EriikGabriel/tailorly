import { Popover } from "@base-ui-components/react/popover";
import { Select } from "@base-ui-components/react/select";
import {
  ArrowDown01Icon,
  ArrowLeft01Icon,
  ArrowRight01Icon,
  CalendarDate1Icon,
  CheckIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { format, parse } from "date-fns";
import { ptBR } from "date-fns/locale/pt-BR";
import { motion, useReducedMotion } from "motion/react";
import { useState } from "react";

const focusClass =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900";
const parseMonth = (month: string) => parse(month, "yyyy-MM", new Date());
const toValue = (year: number, monthIndex: number) =>
  `${String(year).padStart(4, "0")}-${String(monthIndex + 1).padStart(2, "0")}`;

const monthClass = [
  "cursor-pointer rounded-lg border border-transparent bg-surface-container-low px-1 py-2.5 text-center text-xs font-medium leading-[18px] text-primary-950 capitalize",
  "transition-colors duration-150 motion-reduce:transition-none",
  "enabled:not-aria-pressed:hover:bg-surface-container",
  "aria-pressed:bg-primary-900 aria-pressed:text-primary-foreground",
  "disabled:cursor-not-allowed disabled:opacity-30",
  focusClass,
].join(" ");

const yearButtonClass = `flex size-9 items-center justify-center rounded-lg border border-outline-variant/50 bg-card text-primary-900 transition-colors hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-30 ${focusClass}`;

type ReviewMonthPickerProps = {
  id: string;
  label: string;
  value: string;
  min?: string;
  max?: string;
  invalid: boolean;
  describedBy?: string;
  className: string;
  onChange: (value: string) => void;
};

export function ReviewMonthPicker({
  id,
  label,
  value,
  min = "0001-01",
  max = new Date().toISOString().slice(0, 7),
  invalid,
  describedBy,
  className,
  onChange,
}: ReviewMonthPickerProps) {
  const reducedMotion = useReducedMotion();
  const [open, setOpen] = useState(false);

  const selected = value ? parseMonth(value) : null;
  const minDate = parseMonth(min);
  const maxDate = parseMonth(max);
  const minYear = minDate.getFullYear();
  const maxYear = maxDate.getFullYear();

  const clampYear = (year: number) =>
    Math.min(maxYear, Math.max(minYear, year));
  const [viewYear, setViewYear] = useState(() =>
    clampYear(selected?.getFullYear() ?? maxYear),
  );

  const displayValue = selected
    ? format(selected, "MMMM 'de' yyyy", { locale: ptBR })
    : "Selecionar mês e ano";
  const years = Array.from(
    { length: Math.max(0, maxYear - minYear + 1) },
    (_, index) => maxYear - index,
  );

  const handleOpenChange = (next: boolean) => {
    if (next) setViewYear(clampYear(selected?.getFullYear() ?? maxYear));
    setOpen(next);
  };

  return (
    <Popover.Root open={open} onOpenChange={handleOpenChange}>
      <Popover.Trigger
        id={id}
        aria-label={`${label}: ${displayValue}`}
        aria-invalid={invalid}
        aria-describedby={describedBy}
        className={`${className} flex items-center justify-between gap-3 text-left`}
      >
        <span>{displayValue}</span>
        <HugeiconsIcon
          icon={CalendarDate1Icon}
          className="inline-block size-10 zoom-40 shrink-0 mr-4"
          aria-hidden="true"
        />
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Positioner sideOffset={8} align="start" className="z-60">
          <Popover.Popup className="w-80 max-w-[calc(100vw-2rem)] origin-(--transform-origin) rounded-xl border border-outline-variant/50 bg-card text-primary-950 shadow-xl transition-[opacity,transform] duration-150 data-starting-style:scale-95 data-starting-style:opacity-0 data-ending-style:scale-95 data-ending-style:opacity-0 motion-reduce:transition-none">
            <div className="flex items-center gap-3 rounded-t-xl border-b border-outline-variant/40 bg-surface-container-low px-4 py-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-lg border border-primary-200 bg-card text-primary-900">
                <HugeiconsIcon
                  icon={CalendarDate1Icon}
                  className="inline-block size-4 zoom-40 shrink-0 mr-4"
                  aria-hidden="true"
                />
              </span>
              <div>
                <Popover.Title className="text-sm font-semibold">
                  {label}
                </Popover.Title>
                <Popover.Description className="mt-0.5 text-xs text-on-surface-variant">
                  Escolha o ano e depois o mês.
                </Popover.Description>
              </div>
            </div>

            <div className="p-3">
              <div className="mb-3 flex flex-row items-center gap-2 border-b border-outline-variant/30 pb-3 w-full">
                <button
                  type="button"
                  aria-label="Ano anterior"
                  disabled={viewYear <= minYear}
                  onClick={() => setViewYear((year) => clampYear(year - 1))}
                  className={yearButtonClass}
                >
                  <HugeiconsIcon
                    icon={ArrowLeft01Icon}
                    className="size-4"
                    aria-hidden="true"
                  />
                </button>

                <Select.Root
                  value={viewYear}
                  onValueChange={(year) => {
                    if (year !== null) setViewYear(clampYear(year));
                  }}
                >
                  <Select.Trigger
                    aria-label="Selecionar ano"
                    className={`flex h-9 w-full items-center justify-between gap-2 rounded-lg border border-primary-200 bg-surface-container-low px-3 text-primary-950 transition-colors hover:bg-surface-container data-popup-open:border-primary-900 ${focusClass}`}
                  >
                    <Select.Value className="text-sm font-semibold tabular-nums" />
                    <Select.Icon>
                      <HugeiconsIcon
                        icon={ArrowDown01Icon}
                        className="size-3.5 text-primary-900"
                        aria-hidden="true"
                      />
                    </Select.Icon>
                  </Select.Trigger>
                  <Select.Portal>
                    <Select.Positioner
                      alignItemWithTrigger={false}
                      sideOffset={6}
                      className="z-70"
                    >
                      <Select.Popup className="w-(--anchor-width) min-w-32 origin-(--transform-origin) rounded-lg border border-outline-variant/50 bg-card p-1 text-primary-950 shadow-xl outline-none transition-[opacity,transform] duration-150 data-starting-style:translate-y-1 data-starting-style:opacity-0 data-ending-style:opacity-0 motion-reduce:transition-none">
                        <Select.List className="max-h-[min(15rem,var(--available-height))] overflow-y-auto overscroll-contain">
                          {years.map((year) => (
                            <Select.Item
                              key={year}
                              value={year}
                              className="flex cursor-pointer items-center justify-between gap-3 rounded-md px-3 py-2 text-sm tabular-nums outline-none data-highlighted:bg-surface-container data-selected:bg-primary-900 data-selected:text-primary-foreground data-selected:data-highlighted:bg-primary-800"
                            >
                              <Select.ItemText>{year}</Select.ItemText>
                              <Select.ItemIndicator>
                                <HugeiconsIcon
                                  icon={CheckIcon}
                                  className="size-3.5"
                                  aria-hidden="true"
                                />
                              </Select.ItemIndicator>
                            </Select.Item>
                          ))}
                        </Select.List>
                      </Select.Popup>
                    </Select.Positioner>
                  </Select.Portal>
                </Select.Root>

                <button
                  type="button"
                  aria-label="Próximo ano"
                  disabled={viewYear >= maxYear}
                  onClick={() => setViewYear((year) => clampYear(year + 1))}
                  className={yearButtonClass}
                >
                  <HugeiconsIcon
                    icon={ArrowRight01Icon}
                    className="size-4"
                    aria-hidden="true"
                  />
                </button>
              </div>

              <motion.div
                key={viewYear}
                role="group"
                aria-label={`Meses de ${viewYear}`}
                className="grid grid-cols-3 gap-1.5"
                initial={reducedMotion ? false : { opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: reducedMotion ? 0 : 0.16 }}
              >
                {Array.from({ length: 12 }, (_, monthIndex) => {
                  const monthValue = toValue(viewYear, monthIndex);
                  const monthName = format(
                    new Date(viewYear, monthIndex, 1),
                    "LLLL",
                    { locale: ptBR },
                  );
                  const isDisabled = monthValue < min || monthValue > max;
                  const isSelected = monthValue === value;

                  return (
                    <button
                      key={monthValue}
                      type="button"
                      disabled={isDisabled}
                      aria-pressed={isSelected}
                      aria-label={`${monthName} de ${viewYear}`}
                      className={monthClass}
                      onClick={() => {
                        onChange(monthValue);
                        setOpen(false);
                      }}
                    >
                      {monthName}
                    </button>
                  );
                })}
              </motion.div>
            </div>
          </Popover.Popup>
        </Popover.Positioner>
      </Popover.Portal>
    </Popover.Root>
  );
}
