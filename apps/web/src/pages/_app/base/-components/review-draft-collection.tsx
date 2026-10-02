import type { CollectionProps, ReviewField } from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import {
  ChevronDown,
  FileSearch,
  Plus,
  Trash2,
} from "@react-zero-ui/icon-sprite";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { useReducedMotion } from "motion/react";
import { useId, useRef } from "react";
import { ReviewDraftFields } from "./review-draft-fields";

export function ReviewDraftCollection({
  collectionKey,
  records,
  onChange,
  title,
  itemLabel,
  emptyText,
  fields,
  titleKey,
  subtitleKey,
  evidence = false,
}: CollectionProps & {
  title: string;
  itemLabel: string;
  emptyText: string;
  fields: readonly ReviewField[];
  titleKey: string;
  subtitleKey?: string;
  evidence?: boolean;
}) {
  const sourceId = useReviewDraftStore((state) => state.sourceId);
  const expandedId = useReviewDraftStore(
    (state) => state.expandedRecords[collectionKey] ?? null,
  );
  const setExpandedRecord = useReviewDraftStore(
    (state) => state.setExpandedRecord,
  );
  const setExpandedId = (id: string | null) =>
    setExpandedRecord(sourceId, collectionKey, id);
  const id = useId();
  const addRef = useRef<HTMLButtonElement>(null);
  const reducedMotion = useReducedMotion();

  return (
    <section aria-labelledby={`${id}-heading`} className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3
          id={`${id}-heading`}
          className="text-xs font-semibold tracking-[0.6px] uppercase"
        >
          {title}
        </h3>
        <Button
          ref={addRef}
          variant="accent"
          size="sm"
          className="h-auto whitespace-normal rounded-lg text-xs"
          onClick={() => {
            const record = { id: crypto.randomUUID(), fields: {} };
            onChange([...records, record]);
            setExpandedId(record.id);
          }}
        >
          <Plus aria-hidden="true" className="size-3.5" />
          {itemLabel}
        </Button>
      </div>
      <AutoHeight
        deps={[records.length, expandedId]}
        transition={reducedMotion ? { duration: 0 } : undefined}
      >
        <div className="space-y-3 p-0.5">
          {records.length === 0 && (
            <p className="rounded-xl bg-surface-container-low p-4 text-body-sm">
              {emptyText}
            </p>
          )}
          {records.map((record) => {
            const expanded = expandedId === record.id;
            return (
              <article
                key={record.id}
                className="space-y-3 rounded-xl bg-surface-container-low p-4"
              >
                <button
                  type="button"
                  aria-expanded={expanded}
                  aria-controls={`${id}-${record.id}`}
                  onClick={() => setExpandedId(expanded ? null : record.id)}
                  className="flex w-full items-start justify-between gap-3 rounded-md text-left outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span className="min-w-0">
                    <span className="block wrap-break-word text-title-md text-primary-950">
                      {record.fields[titleKey] || "Novo registro"}
                    </span>
                    {subtitleKey && record.fields[subtitleKey] && (
                      <span className="block wrap-break-word text-body-sm">
                        {record.fields[subtitleKey]}
                      </span>
                    )}
                    <span className="mt-1 inline-block rounded-full bg-secondary-container px-2 py-0.5 text-xs font-semibold text-on-secondary-container">
                      Rascunho local • não validado
                    </span>
                  </span>
                  <ChevronDown
                    aria-hidden="true"
                    className={`mt-1 size-4 shrink-0 transition-transform motion-reduce:transition-none ${expanded ? "rotate-180" : ""}`}
                  />
                </button>
                <div id={`${id}-${record.id}`} hidden={!expanded}>
                  <ReviewDraftFields
                    fields={fields}
                    values={record.fields}
                    onChange={(values) =>
                      onChange(
                        records.map((item) =>
                          item.id === record.id
                            ? { ...item, fields: values }
                            : item,
                        ),
                      )
                    }
                  />
                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-outline-variant/30 pt-3">
                    {evidence && (
                      <Button
                        disabled
                        variant="ghost"
                        size="sm"
                        className="h-auto whitespace-normal text-xs"
                        title="Disponível após a extração do documento"
                      >
                        <FileSearch aria-hidden="true" className="size-3.5" />
                        Ver trecho original no OCR
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      className="ml-auto text-xs"
                      onClick={() => {
                        onChange(
                          records.filter((item) => item.id !== record.id),
                        );
                        setExpandedId(null);
                        addRef.current?.focus();
                      }}
                    >
                      <Trash2 aria-hidden="true" className="size-3.5" />
                      Remover rascunho
                    </Button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </AutoHeight>
    </section>
  );
}
