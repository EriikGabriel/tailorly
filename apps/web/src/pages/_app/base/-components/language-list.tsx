import type { CollectionProps } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { IconInput } from "@components/fields/icon-input";
import { ProficiencyField } from "@components/fields/proficiency-field";
import { Languages, Plus, Trash2 } from "@react-zero-ui/icon-sprite";
import { useId } from "react";

const SUGGESTIONS = ["Português", "Inglês", "Espanhol", "Francês", "Alemão"];

/** Idiomas em linhas compactas, com o nível de proficiência em degraus. */
export function LanguageList({ records, onChange }: CollectionProps) {
  const id = useId();
  const present = new Set(
    records.map((record) =>
      (record.fields.language ?? "").trim().toLowerCase(),
    ),
  );

  function add(language = "") {
    onChange([
      ...records,
      { id: crypto.randomUUID(), fields: language ? { language } : {} },
    ]);
  }

  function update(recordId: string, patch: Record<string, string>) {
    onChange(
      records.map((record) =>
        record.id === recordId
          ? { ...record, fields: { ...record.fields, ...patch } }
          : record,
      ),
    );
  }

  return (
    <section aria-labelledby={`${id}-heading`} className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3
          id={`${id}-heading`}
          className="flex items-center gap-2 text-sm font-semibold text-primary-950"
        >
          Idiomas
          <span className="rounded-full bg-surface-container px-2 py-0.5 text-xs font-semibold text-on-surface-variant tabular-nums">
            {records.length}
          </span>
        </h3>
        <Button
          variant="accent"
          size="sm"
          className="h-auto rounded-lg text-xs"
          onClick={() => add()}
        >
          <Plus aria-hidden="true" className="size-3.5" />
          Adicionar idioma
        </Button>
      </div>

      {records.length === 0 && (
        <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed border-outline-variant/60 bg-surface-container-low px-4 py-6 text-center">
          <span className="flex size-10 items-center justify-center rounded-full bg-card text-secondary-700">
            <Languages aria-hidden="true" className="size-5" />
          </span>
          <p className="max-w-md text-sm text-on-surface-variant">
            Nenhum idioma informado. O nível só aparece quando você escolhe um.
          </p>
        </div>
      )}

      <ul className="space-y-2.5">
        {records.map((record) => (
          <li
            key={record.id}
            className="grid gap-3 rounded-xl border border-outline-variant/40 bg-surface-container-low p-3 sm:grid-cols-[minmax(0,13rem)_minmax(0,1fr)_auto] sm:items-center"
          >
            <IconInput
              id={`${id}-${record.id}-language`}
              icon={Languages}
              value={record.fields.language ?? ""}
              placeholder="Idioma"
              onChange={(value) => update(record.id, { language: value })}
            />
            <ProficiencyField
              id={`${id}-${record.id}-level`}
              label={`Nível de ${record.fields.language || "idioma"}`}
              value={record.fields.proficiency ?? ""}
              onChange={(value) => update(record.id, { proficiency: value })}
            />
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Remover ${record.fields.language || "idioma"}`}
              className="justify-self-end text-xs"
              onClick={() =>
                onChange(records.filter((item) => item.id !== record.id))
              }
            >
              <Trash2 aria-hidden="true" className="size-3.5" />
            </Button>
          </li>
        ))}
      </ul>

      <div className="flex flex-wrap items-center gap-1.5">
        <span className="text-xs text-on-surface-variant">
          Adicionar rápido:
        </span>
        {SUGGESTIONS.filter(
          (suggestion) => !present.has(suggestion.toLowerCase()),
        ).map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            onClick={() => add(suggestion)}
            className="rounded-full border border-outline-variant/60 bg-card px-2.5 py-1 text-xs font-medium hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
          >
            + {suggestion}
          </button>
        ))}
      </div>
    </section>
  );
}
