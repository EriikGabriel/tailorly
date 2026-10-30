import { useBaseReview } from "@queries/base-review";
import { useActiveGroundTruth } from "@queries/ground-truth";
import { ArrowRight, FileText, ShieldCheck, ShieldX } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import { durationLabel, isCurrentStatus } from "@/utils/review-format";
import { FactualFidelityGauge } from "./factual-fidelity-gauge";

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" });

export function GroundTruthIntegrityCard() {
  const { hasActiveSnapshot, version, document, facts } =
    useActiveGroundTruth();
  const review = useBaseReview(version?.id);
  const records = review.records.data ?? [];

  if (!hasActiveSnapshot) {
    return (
      <div className="flex w-full flex-col gap-4 rounded-xl bg-card p-6 shadow-xs">
        <div className="flex items-center gap-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-surface-container text-on-surface-variant">
            <ShieldX aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-title-md font-semibold text-primary-950">
              Sem Ground Truth ativo
            </h2>
            <p className="text-label-sm text-on-surface-variant">
              Nenhum Currículo Base aprovado nesta conta
            </p>
          </div>
        </div>
        <Link
          to="/base"
          className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-primary px-4 py-2.5 text-label-md font-semibold text-primary-foreground hover:bg-primary/90"
        >
          Enviar Currículo Base
          <ArrowRight aria-hidden="true" className="size-3.5" />
        </Link>
      </div>
    );
  }

  const experienceDates = (facts?.experience ?? []).filter((e) => e.start);
  const earliestStart = experienceDates
    .map((e) => e.start as string)
    .sort()[0];
  const stillWorking = experienceDates.some((e) => isCurrentStatus(e.status));
  const latestEnd = experienceDates
    .map((e) => e.end)
    .filter((end): end is string => Boolean(end))
    .sort()
    .at(-1);
  const yearsOfExperience = earliestStart
    ? durationLabel(earliestStart, latestEnd, stillWorking)
    : null;

  const totalFacts = records.reduce(
    (sum, record) => sum + record.facts.length,
    0,
  );
  const resolvedFacts = records.reduce(
    (sum, record) =>
      sum + record.facts.filter((f) => f.reviewState !== "pendente").length,
    0,
  );
  const fidelity = totalFacts > 0 ? Math.round((resolvedFacts / totalFacts) * 100) : 100;
  const approvedSections = records.filter(
    (record) => record.facts.every((f) => f.reviewState !== "pendente"),
  ).length;

  return (
    <div className="flex w-full flex-col gap-4 rounded-xl bg-card p-6 shadow-xs">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-secondary-container text-on-secondary-container">
            <ShieldCheck aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-title-md font-semibold text-primary-950">
              Ground Truth Primário
            </h2>
            <p className="text-label-sm text-on-surface-variant">
              Fonte imutável de fatos profissionais
            </p>
          </div>
        </div>
        <span
          aria-hidden="true"
          className="size-3 shrink-0 rounded-full bg-on-secondary-container"
        />
      </div>

      <div className="flex flex-col gap-2 rounded-lg bg-surface-container p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex min-w-0 items-center gap-2">
            <FileText
              aria-hidden="true"
              className="size-5 shrink-0 text-on-surface-variant"
            />
            <div className="min-w-0">
              <p className="truncate text-label-md font-semibold text-primary-950">
                {document?.originalFilename ?? "—"}
              </p>
              <p className="text-label-sm text-on-surface-variant">
                {version?.createdAt
                  ? `Aprovado em ${dateFormat.format(new Date(version.createdAt))}`
                  : ""}
                {document?.pageCount
                  ? ` • ${document.pageCount} página${document.pageCount === 1 ? "" : "s"}`
                  : ""}
              </p>
            </div>
          </div>
        </div>

        {yearsOfExperience && (
          <div className="flex items-center justify-between pt-2 text-label-sm text-on-surface-variant">
            <span>{yearsOfExperience} de experiência mapeada</span>
          </div>
        )}
      </div>

      <div className="flex items-center gap-4 rounded-lg bg-surface-container-low p-2">
        <FactualFidelityGauge value={fidelity} />
        <div>
          <p className="text-title-md font-semibold text-primary-950">
            Taxa de Fidelidade Factual
          </p>
          <p className="text-body-sm text-on-surface-variant">
            % de fatos extraídos já confirmados, corrigidos ou omitidos na
            revisão — nada pendente é usado nas gerações.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Link
          to="/base"
          className="inline-flex w-full items-center justify-center gap-1.5 rounded-lg bg-primary px-4 py-2.5 text-label-md font-semibold text-primary-foreground hover:bg-primary/90"
        >
          <FileText aria-hidden="true" className="size-4" />
          Abrir Curadoria &amp; Auditoria Factual
          <ArrowRight aria-hidden="true" className="size-3.5" />
        </Link>

        <div className="flex items-center justify-between px-2 text-label-sm text-on-surface-variant">
          <span>
            Fatos revisados:{" "}
            <span className="text-on-secondary-container">
              {resolvedFacts}/{totalFacts}
            </span>
          </span>
          <span className="inline-flex items-center gap-1">
            <span
              aria-hidden="true"
              className="size-2 rounded-full bg-on-secondary-container"
            />
            {approvedSections}/{records.length} seções aprovadas
          </span>
        </div>
      </div>
    </div>
  );
}
