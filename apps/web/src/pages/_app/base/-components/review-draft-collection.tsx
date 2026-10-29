import type {
  CollectionProps,
  DraftFields,
  ReviewField,
} from "@@types/review-model";
import {
  Accordion,
  AccordionItem,
  AccordionPanel,
  AccordionTrigger,
} from "@animate/base/accordion";
import { Button } from "@animate/buttons/button";
import {
  FileSearch,
  type IconProps,
  Plus,
  Trash2,
} from "@react-zero-ui/icon-sprite";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { type ComponentType, useId, useRef } from "react";
import { usePendingFields, VerifiedMark } from "./pending-fields";
import type { BadgeTone, RecordSummary } from "./record-summaries";
import { ReviewDraftFields } from "./review-draft-fields";

const badgeTone: Record<BadgeTone, string> = {
  success:
    "bg-on-tertiary-container/15 border border-on-tertiary-container text-on-tertiary-container",
  info: "bg-secondary-container text-on-secondary-container",
  neutral: "border border-outline-variant/50 bg-card text-on-surface-variant",
};

export function ReviewDraftCollection({
  collectionKey,
  records,
  onChange,
  title,
  itemLabel,
  emptyText,
  fields,
  summarize,
  icon: Icon,
  evidence = false,
}: CollectionProps & {
  title: string;
  itemLabel: string;
  emptyText: string;
  fields: readonly ReviewField[];
  summarize: (values: DraftFields) => RecordSummary;
  icon: ComponentType<IconProps>;
  evidence?: boolean;
}) {
  const { sourceId, expandedRecords, setExpandedRecord } =
    useReviewDraftStore();
  const expandedId = expandedRecords[collectionKey] ?? null;
  const setExpandedId = (id: string | null) =>
    setExpandedRecord(sourceId, collectionKey, id);
  const id = useId();
  const addRef = useRef<HTMLButtonElement>(null);
  const pending = usePendingFields();

  function addRecord() {
    const record = { id: crypto.randomUUID(), fields: {} };
    onChange([...records, record]);
    setExpandedId(record.id);
  }

  return (
    <section aria-labelledby={`${id}-heading`} className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3
          id={`${id}-heading`}
          className="flex items-center gap-2 text-sm font-semibold text-primary-950"
        >
          {title}
          <span className="rounded-full bg-surface-container px-2 py-0.5 text-xs font-semibold text-on-surface-variant tabular-nums">
            {records.length}
          </span>
        </h3>
        <Button
          ref={addRef}
          variant="accent"
          size="sm"
          className="h-auto whitespace-normal rounded-lg text-xs"
          onClick={addRecord}
        >
          <Plus aria-hidden="true" className="size-3.5" />
          {itemLabel}
        </Button>
      </div>

      {records.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed border-outline-variant/60 bg-surface-container-low px-4 py-6 text-center">
          <span className="flex size-10 items-center justify-center rounded-full bg-card text-secondary-700">
            <Icon aria-hidden="true" className="size-5" />
          </span>
          <p className="max-w-md text-sm text-on-surface-variant">
            {emptyText}
          </p>
        </div>
      ) : (
        <Accordion
          value={expandedId ? [expandedId] : []}
          onValueChange={(next: string[]) =>
            setExpandedId(next.find((item) => item !== expandedId) ?? null)
          }
          className="space-y-2.5 pb-1"
        >
          {records.map((record) => {
            const summary = summarize(record.fields);
            const rowPending = pending.row(record.id);
            const rowRelevant = rowPending.filter(
              (issue) => issue.jobRelevance,
            );
            return (
              <AccordionItem
                key={record.id}
                value={record.id}
                className={`overflow-clip rounded-xl border border-b! transition-shadow data-open:shadow-sm ${rowPending.length > 0 ? "border-tertiary-200 bg-tertiary-50" : "border-outline-variant/40"}`}
              >
                <AccordionTrigger
                  className={`items-center gap-3 rounded-none px-4 py-3 no-underline hover:no-underline ${rowPending.length > 0 ? "hover:bg-tertiary-100" : "hover:bg-surface-container"}`}
                >
                  <span className="flex min-w-0 flex-1 items-center gap-3">
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-card text-secondary-700 shadow-xs">
                      <Icon aria-hidden="true" className="size-5" />
                    </span>
                    <span className="min-w-0 space-y-0.5">
                      <span className="block wrap-break-word text-sm font-semibold text-primary-950">
                        {summary.title}
                      </span>
                      {summary.subtitle && (
                        <span className="block wrap-break-word text-sm text-on-surface-variant">
                          {summary.subtitle}
                        </span>
                      )}
                      {summary.meta && (
                        <span className="block text-xs text-on-surface-variant">
                          {summary.meta}
                        </span>
                      )}
                    </span>
                  </span>
                  <span className="flex shrink-0 flex-wrap items-center justify-end gap-1.5">
                    {rowPending.length > 0 && (
                      <span className="rounded-full bg-tertiary-100 px-2 py-0.5 text-[11px] font-bold text-tertiary-900">
                        {rowPending.length}{" "}
                        {rowPending.length === 1 ? "pendência" : "pendências"}
                        {rowRelevant.length > 0
                          ? ` · ${rowRelevant.length} p/ a vaga`
                          : ""}
                      </span>
                    )}
                    {summary.badges.map((badge) => (
                      <span
                        key={badge.label}
                        className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${badgeTone[badge.tone]}`}
                      >
                        {badge.label}
                      </span>
                    ))}

                    {rowPending.length === 0 &&
                      pending.rowVerified(record.id) && (
                        <VerifiedMark label="Verificado" />
                      )}
                  </span>
                </AccordionTrigger>
                <AccordionPanel
                  className={`space-y-3 border-t border-outline-variant/30 px-4 pt-4 pb-6 ${rowPending.length > 0 ? "bg-tertiary-50" : "bg-card/60"}`}
                >
                  <ReviewDraftFields
                    scope={record.id}
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
                  <div className="flex flex-wrap items-center justify-between gap-2 border-t border-outline-variant/30 pt-3">
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
                </AccordionPanel>
              </AccordionItem>
            );
          })}
        </Accordion>
      )}
    </section>
  );
}
