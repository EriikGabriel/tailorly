import type { CvGenerationResponse } from "@lib/api/generated/model";

const aiStatusLabel: Record<string, string> = {
  completed: "IA ajustou a apresentação",
  failed: "A IA falhou; regras simples assumiram",
  unavailable: "Sem IA configurada; só regras simples",
  skipped: "Otimização por IA desligada",
};

/** Nota de ATS estimada pela esteira, o que pesou nela e o que a vaga pediu e o currículo não tem. */
export function AtsScoreCard({
  generation,
}: {
  generation: CvGenerationResponse;
}) {
  const report = generation.ats?.report;
  if (!report) return null;
  const score = report.score ?? 0;
  const iterations = generation.ats?.iterations ?? [];

  return (
    <section
      aria-labelledby="ats-heading"
      className="flex flex-col gap-3 rounded-xl bg-card p-4 shadow-xs"
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3
            id="ats-heading"
            className="text-sm font-semibold text-on-surface"
          >
            Nota de ATS estimada
          </h3>
          <p className="text-xs text-on-surface-variant">
            Estimativa por regras sobre o texto do próprio PDF, não de um ATS
            real.
          </p>
        </div>
        <span className="text-3xl font-semibold tabular-nums text-primary-900">
          <span className="sr-only">{score} de 100</span>
          {score}
          <span className="text-sm font-medium text-on-surface-variant">
            /100
          </span>
        </span>
      </div>

      <ul className="flex flex-col gap-1.5">
        {(report.checks ?? []).map((check) => (
          <li key={check.id} className="flex flex-col gap-0.5">
            <div className="flex justify-between text-xs">
              <span className="font-medium text-on-surface">{check.label}</span>
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

      {(report.missingTerms?.length ?? 0) > 0 && (
        <p className="rounded-lg bg-surface-container-low p-2.5 text-xs leading-5 text-on-surface-variant">
          <strong className="font-semibold text-on-surface">
            A vaga pede e seu Currículo Base não afirma:
          </strong>{" "}
          {report.missingTerms?.join(", ")}. Não os acrescentamos; se forem
          verdade, inclua-os no Currículo Base e revise.
        </p>
      )}

      <div className="text-xs text-on-surface-variant">
        <p className="font-semibold text-on-surface">
          {aiStatusLabel[generation.ats?.aiStatus ?? ""] ??
            generation.ats?.aiStatus}
        </p>
        <ol className="mt-1 flex flex-col gap-0.5">
          {iterations.map((iteration) => (
            <li key={iteration.number}>
              Tentativa {iteration.number} ({iteration.source}):{" "}
              {iteration.score}
              {iteration.rationale ? ` — ${iteration.rationale}` : ""}
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
