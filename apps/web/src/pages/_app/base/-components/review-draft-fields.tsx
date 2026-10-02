import type { DraftFields, ReviewField } from "@@types/review-model";
import { useId } from "react";

export function ReviewDraftFields({
  fields,
  values,
  onChange,
}: {
  fields: readonly ReviewField[];
  values: DraftFields;
  onChange: (values: DraftFields) => void;
}) {
  const id = useId();
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {fields.map((field) => {
        const inputId = `${id}-${field.key}`;
        const props = {
          id: inputId,
          value: values[field.key] ?? "",
          onChange: (
            event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
          ) => onChange({ ...values, [field.key]: event.target.value }),
          "aria-describedby": field.hint ? `${inputId}-hint` : undefined,
          className:
            "w-full min-w-0 rounded-md border border-transparent bg-transparent px-1 py-1 text-base font-semibold leading-6 text-primary-950 outline-none placeholder:font-normal placeholder:text-outline focus:border-outline-variant focus:bg-card focus:ring-2 focus:ring-ring/20",
          placeholder: "Não informado",
        };
        return (
          <div
            key={field.key}
            className={`min-w-0 space-y-0.5 rounded-xl bg-surface-container-low p-2 ${field.wide ? "sm:col-span-2" : ""}`}
          >
            <label
              htmlFor={inputId}
              className="block text-xs font-semibold leading-4 tracking-[0.6px] uppercase"
            >
              {field.label}
            </label>
            {field.type === "textarea" ? (
              <textarea {...props} rows={3} />
            ) : (
              <input {...props} type={field.type ?? "text"} />
            )}
            {field.hint && (
              <p
                id={`${inputId}-hint`}
                className="text-body-sm text-muted-foreground"
              >
                {field.hint}
              </p>
            )}
          </div>
        );
      })}
    </div>
  );
}
