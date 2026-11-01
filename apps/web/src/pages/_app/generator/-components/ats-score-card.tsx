import { AnimatedBar } from "@components/animated-bar";
import type { CvGenerationResponse } from "@lib/api/generated/model";
import { useState } from "react";
import { atsScoreColor, atsScoreLabel } from "@/utils/ats-score";
import { AnimatedScore } from "./animated-score";
import { AtsDetails, type AtsTab } from "./ats-details";
import { AtsReviewSection } from "./ats-review-section";

/**
 * Nota de ATS estimada em formato compacto: a nota, o que mais pesou contra e a leitura da IA. A
 * análise completa (todas as checagens, requisito por requisito e as tentativas da esteira) abre num
 * diálogo, para a coluna não virar uma lista longa.
 */
export function AtsScoreCard({
  generation,
}: {
  generation: CvGenerationResponse;
}) {
  const [expanded, setExpanded] = useState(false);
  const [tab, setTab] = useState<AtsTab>("rules");
  const report = generation.ats?.report;
  if (!report) return null;
  const score = report.score ?? 0;
  const scoreColor = atsScoreColor(score);
  const weak = (report.checks ?? [])
    .filter((check) => (check.points ?? 0) < (check.maxPoints ?? 0))
    .sort(
      (left, right) =>
        (right.maxPoints ?? 0) -
        (right.points ?? 0) -
        ((left.maxPoints ?? 0) - (left.points ?? 0)),
    )
    .slice(0, 2);

  return (
    <section
      aria-labelledby="ats-heading"
      className="flex flex-col gap-3 rounded-xl bg-card p-4 shadow-xs"
    >
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h3
            id="ats-heading"
            className="text-sm font-semibold text-on-surface"
          >
            Nota de ATS estimada
          </h3>
          <p className="text-xs text-on-surface-variant">
            {weak.length === 0
              ? "Todas as checagens por regras estão completas."
              : `Mais pesou: ${weak
                  .map(
                    (check) =>
                      `${check.label} (${check.points}/${check.maxPoints})`,
                  )
                  .join(" · ")}`}
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-end">
          <span
            className="text-3xl font-semibold tabular-nums"
            style={{ color: scoreColor }}
          >
            <span className="sr-only">{score} de 100</span>
            <span aria-hidden="true">
              <AnimatedScore value={score} />
            </span>
            <span
              aria-hidden="true"
              className="text-sm font-medium text-on-surface-variant"
            >
              /100
            </span>
          </span>
          <span className="text-xs font-semibold" style={{ color: scoreColor }}>
            {atsScoreLabel(score)}
          </span>
        </div>
      </div>

      <AnimatedBar
        value={score}
        max={100}
        label="Nota de ATS estimada"
        color={scoreColor}
        className="h-2"
      />

      <AtsReviewSection
        generation={generation}
        onOpenDetails={() => {
          setTab("ai");
          setExpanded(true);
        }}
      />

      <button
        type="button"
        aria-expanded={expanded}
        aria-controls="ats-details"
        onClick={() => setExpanded((value) => !value)}
        className="w-fit rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high"
      >
        {expanded ? "Ocultar análise" : "Ver análise completa"}
      </button>

      {expanded && (
        <div
          id="ats-details"
          className="border-t border-outline-variant/50 pt-3"
        >
          <AtsDetails generation={generation} tab={tab} onTabChange={setTab} />
        </div>
      )}
    </section>
  );
}
