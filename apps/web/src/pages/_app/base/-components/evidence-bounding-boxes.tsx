import type { ReviewStepId } from "@@types/review-model";
import type { FactRecord } from "@queries/base-review";
import { ScanSearch } from "@react-zero-ui/icon-sprite";
import { motion } from "motion/react";
import type { ReactNode } from "react";
import { factFieldLabel, formatFactValue } from "@/utils/fact-display";
import {
  categoryLabel,
  EVIDENCE_TONE,
  isCategoryActive,
} from "./evidence-style";

function highlightExcerpt(excerpt: string, value: string): ReactNode {
  const needle = value.trim();
  if (!needle) return excerpt;
  const index = excerpt.toLowerCase().indexOf(needle.toLowerCase());
  if (index === -1) return excerpt;
  return (
    <>
      {excerpt.slice(0, index)}
      <mark className="rounded bg-error/20 px-1 font-semibold text-on-surface">
        {excerpt.slice(index, index + needle.length)}
      </mark>
      {excerpt.slice(index + needle.length)}
    </>
  );
}

/** Alternativa textual para arquivos que não são PDF ou quando a prévia local não está disponível. */
export function EvidenceBoundingBoxes({
  records,
  activeStep,
}: {
  records: FactRecord[];
  activeStep: ReviewStepId;
}) {
  const visibleRecords = records.filter((record) =>
    record.facts.some((fact) => fact.reviewState !== "omitido"),
  );

  if (visibleRecords.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 py-12 text-center">
        <span className="flex size-14 items-center justify-center rounded-xl bg-surface-container-low text-muted-foreground">
          <ScanSearch aria-hidden="true" className="size-7" />
        </span>
        <div className="max-w-72 space-y-1">
          <h3 className="text-title-md">Nenhum ponto de extração ainda</h3>
          <p className="text-body-sm">
            Os pontos de origem aparecem aqui assim que a extração confirmar
            fatos no documento.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col gap-4 overflow-y-auto p-4 sm:p-6">
      {visibleRecords.map((record) => {
        const isActive = isCategoryActive(record.category, activeStep);
        const label = categoryLabel(record.category);
        const facts = record.facts.filter(
          (fact) => fact.reviewState !== "omitido",
        );

        return (
          <motion.div
            key={record.id}
            layout
            initial={false}
            animate={{ opacity: isActive ? 1 : 0.65 }}
            transition={{ duration: 0.3, ease: "easeOut" }}
            className={`relative rounded-xl p-3.5 transition-colors ${
              isActive ? EVIDENCE_TONE.box.focused : EVIDENCE_TONE.box.neutral
            }`}
          >
            <span
              className={`absolute -top-2.5 right-3 inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-semibold tracking-[0.3px] shadow-xs ${
                isActive
                  ? EVIDENCE_TONE.badge.focused
                  : EVIDENCE_TONE.badge.neutral
              }`}
            >
              {label} {isActive ? "• Em foco" : "mapeado"}
            </span>

            <ul className="flex flex-col gap-2 pt-1.5">
              {facts.map((fact) => {
                const label = factFieldLabel(fact.fieldKey, record.category);
                const value = formatFactValue(
                  fact.reviewedValue ?? fact.extractedValue,
                );
                const evidence = fact.evidence[0];

                return (
                  <li
                    key={fact.id}
                    className="text-[13px] leading-5 text-on-surface"
                  >
                    <span className="font-semibold text-primary-950">
                      {label}:{" "}
                    </span>
                    {evidence?.sourceExcerpt ? (
                      <span className="text-on-surface-variant">
                        {highlightExcerpt(evidence.sourceExcerpt, value)}
                      </span>
                    ) : value ? (
                      <span className="text-on-surface-variant">{value}</span>
                    ) : (
                      <span className="italic text-on-surface-variant/70">
                        sem evidência vinculada
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          </motion.div>
        );
      })}
    </div>
  );
}
