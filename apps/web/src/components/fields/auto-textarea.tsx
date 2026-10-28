import { useLayoutEffect, useRef } from "react";
import { controlClass } from "./field-styles";

interface AutoTextareaProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  maxLength?: number;
  placeholder?: string;
  describedBy?: string;
  minRows?: number;
}

/** Textarea que cresce com o conteúdo, com contador quando há limite. */
export function AutoTextarea({
  id,
  value,
  onChange,
  maxLength,
  placeholder = "Não informado",
  describedBy,
  minRows = 3,
}: AutoTextareaProps) {
  const ref = useRef<HTMLTextAreaElement>(null);

  // biome-ignore lint/correctness/useExhaustiveDependencies: reajusta a altura a cada mudança do texto.
  useLayoutEffect(() => {
    const element = ref.current;
    if (!element) return;
    element.style.height = "auto";
    element.style.height = `${element.scrollHeight}px`;
  }, [value]);

  return (
    <div className={`${controlClass} flex-col items-stretch gap-1 py-2`}>
      <textarea
        ref={ref}
        id={id}
        rows={minRows}
        value={value}
        maxLength={maxLength}
        placeholder={placeholder}
        aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)}
        className="min-h-0 w-full resize-none bg-transparent text-sm leading-6 font-medium text-primary-950 outline-none placeholder:font-normal placeholder:text-outline"
      />
      {maxLength !== undefined && (
        <span
          aria-hidden="true"
          className="self-end text-[11px] text-on-surface-variant tabular-nums"
        >
          {value.length}/{maxLength}
        </span>
      )}
    </div>
  );
}
