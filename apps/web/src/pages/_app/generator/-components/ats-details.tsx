import {
  Tabs,
  TabsList,
  TabsPanel,
  TabsPanels,
  TabsTab,
} from "@animate/base/tabs";
import type { CvGenerationResponse } from "@lib/api/generated/model";
import { AtsReviewDetails } from "./ats-review-section";

const aiStatusLabel: Record<string, string> = {
  completed: "IA ajustou a apresentação",
  failed: "A IA falhou; regras simples assumiram",
  unavailable: "Sem IA configurada; só regras simples",
  skipped: "Otimização por IA desligada",
};

const TAB_CLASS =
  "h-8 px-3 text-sm text-on-surface-variant data-selected:font-semibold data-selected:text-primary-900";

export type AtsTab = "rules" | "ai" | "attempts";

/** Detalhe do ATS, em abas: a nota por regras, a leitura da IA e as tentativas da esteira. */
export function AtsDetails({
  generation,
  tab,
  onTabChange,
}: {
  generation: CvGenerationResponse;
  tab: AtsTab;
  onTabChange: (tab: AtsTab) => void;
}) {
  const report = generation.ats?.report;
  const iterations = generation.ats?.iterations ?? [];
  const review = generation.atsReview;

  return (
    <Tabs
      value={tab}
      onValueChange={(value) => onTabChange(value as AtsTab)}
      className="gap-3"
    >
      <TabsList
        aria-label="Seções da análise de ATS"
        className="h-auto bg-surface-container p-1"
      >
        <TabsTab value="rules" type="button" className={TAB_CLASS}>
          Nota por regras
        </TabsTab>
        <TabsTab value="ai" type="button" className={TAB_CLASS}>
          Leitura da IA
        </TabsTab>
        <TabsTab value="attempts" type="button" className={TAB_CLASS}>
          Tentativas
        </TabsTab>
      </TabsList>

      <TabsPanels>
        <TabsPanel value="rules" className="flex flex-col gap-3">
          <ul className="flex flex-col gap-2">
            {(report?.checks ?? []).map((check) => (
              <li key={check.id} className="flex flex-col gap-0.5">
                <div className="flex justify-between text-xs">
                  <span className="font-medium text-on-surface">
                    {check.label}
                  </span>
                  <span className="tabular-nums text-on-surface-variant">
                    {check.points}/{check.maxPoints}
                  </span>
                </div>
                <progress
                  className="h-1.5 w-full overflow-hidden rounded-full accent-primary-900"
                  value={check.points}
                  max={check.maxPoints}
                  aria-label={check.label}
                />
                <span className="text-[11px] text-on-surface-variant">
                  {check.detail}
                </span>
              </li>
            ))}
          </ul>
          {(report?.missingTerms?.length ?? 0) > 0 && (
            <p className="rounded-lg bg-surface-container-low p-2.5 text-xs leading-5 text-on-surface-variant">
              <strong className="font-semibold text-on-surface">
                A vaga pede e seu Currículo Base não afirma:
              </strong>{" "}
              {report?.missingTerms?.join(", ")}. Não os acrescentamos; se forem
              verdade, inclua-os no Currículo Base e revise.
            </p>
          )}
        </TabsPanel>

        <TabsPanel value="ai">
          {review ? (
            <AtsReviewDetails review={review} />
          ) : (
            <p className="text-sm text-on-surface-variant">
              Ainda sem leitura da IA. Use “Analisar com IA” no card de ATS.
            </p>
          )}
        </TabsPanel>

        <TabsPanel value="attempts" className="text-xs text-on-surface-variant">
          <p className="font-semibold text-on-surface">
            {aiStatusLabel[generation.ats?.aiStatus ?? ""] ??
              generation.ats?.aiStatus}
          </p>
          <ol className="mt-1 flex flex-col gap-1">
            {iterations.map((iteration) => (
              <li key={iteration.number}>
                Tentativa {iteration.number} ({iteration.source}):{" "}
                {iteration.score}
                {iteration.rationale ? ` — ${iteration.rationale}` : ""}
              </li>
            ))}
          </ol>
        </TabsPanel>
      </TabsPanels>
    </Tabs>
  );
}
