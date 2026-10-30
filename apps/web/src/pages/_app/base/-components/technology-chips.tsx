import type { CollectionProps } from "@@types/review-model";
import { AutoTextarea } from "@components/fields/auto-textarea";
import { IconInput } from "@components/fields/icon-input";
import { Check, Tag, X } from "@react-zero-ui/icon-sprite";
import { useId, useState } from "react";
import { usePendingFields } from "../-hooks/use-pending-fields";
import { PendingNote } from "./pending-note";
import { VerifiedMark } from "./verified-mark";

/** Tecnologias e competências como chips; clicar em um chip edita o contexto de uso. */
export function TechnologyChips({ records, onChange }: CollectionProps) {
  const id = useId();
  const pending = usePendingFields();
  const [draft, setDraft] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = records.find((record) => record.id === selectedId) ?? null;
  const selectedIssue = selected
    ? pending.field(selected.id, "name")
    : undefined;

  function addFromText(text: string) {
    const known = new Set(
      records.map((record) => (record.fields.name ?? "").trim().toLowerCase()),
    );
    const additions = text
      .split(/[,;\n]/)
      .map((part) => part.trim())
      .filter((part) => {
        const key = part.toLowerCase();
        if (!part || known.has(key)) return false;
        known.add(key);
        return true;
      })
      .map((name) => ({ id: crypto.randomUUID(), fields: { name } }));
    if (additions.length > 0) onChange([...records, ...additions]);
    setDraft("");
  }

  return (
    <section aria-labelledby={`${id}-heading`} className="space-y-3">
      <h3
        id={`${id}-heading`}
        className="flex items-center gap-2 text-sm font-semibold text-primary-950"
      >
        Tecnologias e competências
        <span className="rounded-full bg-surface-container px-2 py-0.5 text-xs font-semibold text-on-surface-variant tabular-nums">
          {records.length}
        </span>
      </h3>

      <div className="space-y-2 rounded-xl border border-outline-variant/40 bg-surface-container-low p-3">
        {records.length === 0 ? (
          <p className="text-sm text-on-surface-variant">
            Nenhuma tecnologia extraída. Digite abaixo e pressione Enter para
            adicionar; separe várias com vírgula.
          </p>
        ) : (
          <ul className="flex flex-wrap gap-1.5">
            {records.map((record) => {
              const name = record.fields.name || "Sem nome";
              const active = record.id === selectedId;
              const issue = pending.field(record.id, "name");
              return (
                <li
                  key={record.id}
                  title={issue?.jobRelevance?.label}
                  className={`flex items-center overflow-hidden rounded-full border text-xs font-semibold transition-colors ${active ? "border-primary-900 bg-primary-900 text-primary-foreground" : "border-outline-variant/60 bg-card text-primary-950"} ${issue && !active ? "bg-tertiary-50" : ""}`}
                >
                  <button
                    type="button"
                    aria-pressed={active}
                    aria-label={`Editar contexto de ${name}`}
                    onClick={() => setSelectedId(active ? null : record.id)}
                    className="flex items-center gap-1.5 py-1 pr-1.5 pl-3 outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {name}
                    {record.fields.context && (
                      <span
                        aria-hidden="true"
                        className="size-1.5 rounded-full bg-current opacity-60"
                      />
                    )}
                  </button>
                  {!issue && pending.verified(record.id, "name") && (
                    <VerifiedMark
                      label={`${name}: verificado`}
                      className="pr-1"
                    />
                  )}
                  {issue && !pending.edited(issue) && (
                    <button
                      type="button"
                      aria-label={`Confirmar ${name}`}
                      title="Confirmar tecnologia"
                      disabled={pending.busy}
                      onClick={() => pending.confirm(issue)}
                      className="px-1.5 py-1 text-tertiary-900 outline-none hover:opacity-100 focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                    >
                      <Check aria-hidden="true" className="size-3.5" />
                    </button>
                  )}
                  <button
                    type="button"
                    aria-label={`Remover ${name}`}
                    onClick={() => {
                      onChange(records.filter((item) => item.id !== record.id));
                      if (active) setSelectedId(null);
                    }}
                    className="px-2 py-1 opacity-70 outline-none hover:opacity-100 focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <X aria-hidden="true" className="size-3" />
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        <IconInput
          id={`${id}-add`}
          icon={Tag}
          value={draft}
          placeholder="Adicionar tecnologia (Enter)"
          onEnter={() => addFromText(draft)}
          onChange={(value) =>
            value.endsWith(",") || value.endsWith(";")
              ? addFromText(value)
              : setDraft(value)
          }
          trailing={
            draft.trim() && (
              <button
                type="button"
                onClick={() => addFromText(draft)}
                className="rounded-md px-2 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container"
              >
                Adicionar
              </button>
            )
          }
        />
      </div>

      {selected && (
        <div className="space-y-1.5 rounded-xl bg-surface-container-low p-3">
          {selectedIssue && <PendingNote issue={selectedIssue} />}
          <label
            htmlFor={`${id}-context`}
            className="text-xs font-semibold tracking-[0.4px] text-on-surface-variant uppercase"
          >
            Contexto de uso — {selected.fields.name}
          </label>
          <AutoTextarea
            id={`${id}-context`}
            value={selected.fields.context ?? ""}
            minRows={2}
            maxLength={300}
            placeholder="Experiência ou projeto em que foi utilizada"
            onChange={(value) =>
              onChange(
                records.map((record) =>
                  record.id === selected.id
                    ? {
                        ...record,
                        fields: { ...record.fields, context: value },
                      }
                    : record,
                ),
              )
            }
          />
        </div>
      )}
    </section>
  );
}
