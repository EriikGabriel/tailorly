import { CountingNumber } from "@animate/primitives/texts/counting-number";
import { useReducedMotion } from "motion/react";
import { useState } from "react";
import type { CorrectionField } from "@/types/review";
import { ReviewMonthPicker } from "./review-month-picker";

const inputClass =
  "mt-1.5 block w-full max-w-64 rounded-md border border-outline-variant/60 bg-card px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900";

type ReviewCorrectionInputProps = {
  id: string;
  field: CorrectionField;
  value: string;
  invalid: boolean;
  onChange: (value: string) => void;
};

function ReviewNumberInput({
  id,
  field,
  value,
  invalid,
  onChange,
}: ReviewCorrectionInputProps) {
  const reducedMotion = useReducedMotion();
  const [focused, setFocused] = useState(false);
  const [preview, setPreview] = useState<{
    from: number;
    number: number;
    decimals: number;
  } | null>(null);

  function commit(input: HTMLInputElement) {
    const number = input.valueAsNumber;
    if (
      !input.value.trim() ||
      !input.validity.valid ||
      !Number.isFinite(number) ||
      invalid
    ) {
      setPreview(null);
      return;
    }
    const original = Number(field.originalValue);
    const decimals =
      new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 20 })
        .formatToParts(number)
        .find((part) => part.type === "fraction")?.value.length ?? 0;
    setPreview((previous) => ({
      from:
        previous?.number ??
        (field.originalValue && Number.isFinite(original) ? original : 0),
      number,
      decimals,
    }));
  }

  return (
    <>
      <input
        id={id}
        type="number"
        min={field.min}
        max={field.max}
        step="any"
        placeholder={field.placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={(event) => {
          setFocused(false);
          commit(event.currentTarget);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            event.currentTarget.blur();
          }
        }}
        aria-invalid={invalid}
        aria-describedby={invalid ? `${id}-error` : undefined}
        className={inputClass}
      />
      {preview && !focused && !invalid && (
        <p className="mt-2 text-xs text-on-surface-variant" role="status">
          Valor informado:{" "}
          <span className="sr-only">
            {preview.number.toLocaleString("pt-BR", {
              maximumFractionDigits: 20,
            })}
          </span>
          <span
            aria-hidden="true"
            className="font-semibold tabular-nums text-primary-950"
          >
            {reducedMotion ? (
              preview.number.toLocaleString("pt-BR", {
                maximumFractionDigits: 20,
              })
            ) : (
              <CountingNumber
                number={preview.number}
                fromNumber={preview.from}
                decimalPlaces={preview.decimals}
                decimalSeparator=","
                transition={{ stiffness: 180, damping: 30 }}
              />
            )}
          </span>
        </p>
      )}
    </>
  );
}

export function ReviewCorrectionInput(props: ReviewCorrectionInputProps) {
  const { id, field, value, invalid, onChange } = props;
  const describedBy = invalid ? `${id}-error` : undefined;

  return (
    <div className="px-1.5 pt-3  pb-1.5 text-xs font-medium">
      <label htmlFor={id}>{field.label}</label>
      {field.type === "month" ? (
        <ReviewMonthPicker
          id={id}
          label={field.label}
          value={value}
          min={field.min}
          max={field.max}
          invalid={invalid}
          describedBy={describedBy}
          className={inputClass}
          onChange={onChange}
        />
      ) : field.type === "number" ? (
        <ReviewNumberInput {...props} />
      ) : field.type === "select" ? (
        <select
          id={id}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-invalid={invalid}
          aria-describedby={describedBy}
          className={inputClass}
        >
          <option value="">Selecione uma opção</option>
          {field.options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          id={id}
          type={field.type}
          placeholder={field.placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-invalid={invalid}
          aria-describedby={describedBy}
          className={inputClass}
        />
      )}
      {invalid && (
        <span id={`${id}-error`} className="mt-1 block text-secondary-700">
          Informe um valor válido e diferente do original.
        </span>
      )}
    </div>
  );
}
