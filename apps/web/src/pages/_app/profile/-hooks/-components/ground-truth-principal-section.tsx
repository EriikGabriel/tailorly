import { Button } from "@animate/buttons/button";
import {
  Award,
  Briefcase,
  ClipboardCheck,
  GraduationCap,
  Settings2,
} from "@react-zero-ui/icon-sprite";
import { previewGroundTruthFacts } from "@/data/profile-preview";
import { SectionCard } from "./section-card";

export function GroundTruthPrincipalSection() {
  const facts = previewGroundTruthFacts;

  return (
    <SectionCard
      icon={ClipboardCheck}
      title="2. Ground Truth Principal (Verdade Absoluta)"
      description="Todos os fatos foram auditados contra o PDF mestre. A IA é matematicamente restrita a usar apenas dados validados nesta curadoria."
      trailing={
        <Button size="lg">
          <Settings2 aria-hidden="true" className="size-4" />
          Curar &amp; Auditar Dossiê de Fatos (Split-View)
        </Button>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2 rounded-lg bg-surface-container-low p-4">
          <div className="flex items-center gap-2">
            <ClipboardCheck
              aria-hidden="true"
              className="size-4 text-on-surface-variant"
            />
            <h3 className="text-title-md font-semibold text-primary-950">
              Resumo Executivo Base
            </h3>
            <span className="ml-auto rounded bg-card px-2 py-0.5 text-label-sm font-medium text-on-secondary-container">
              Validado
            </span>
          </div>
          <p className="text-body-sm text-on-surface-variant">
            &quot;{facts.executiveSummary}&quot;
          </p>
        </div>

        <div className="flex flex-col gap-3 rounded-lg bg-surface-container-low p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Briefcase
                aria-hidden="true"
                className="size-4 text-on-surface-variant"
              />
              <h3 className="text-title-md font-semibold text-primary-950">
                Histórico Profissional Mapeado ({facts.totalPositions} Posições)
              </h3>
            </div>
            <span className="text-label-sm font-medium text-on-surface-variant">
              {facts.timelineRangeLabel}
            </span>
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            {facts.positions.map((position) => (
              <div
                key={position.role}
                className="flex flex-col gap-1 rounded-lg bg-card p-3"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-label-md font-semibold text-primary-950">
                    {position.role}
                  </span>
                  <span className="shrink-0 text-label-sm font-medium text-on-secondary-container">
                    {position.period}
                  </span>
                </div>
                <p className="text-body-sm text-on-surface-variant">
                  {position.company}
                </p>
                <p className="text-label-sm font-medium text-on-surface-variant">
                  {position.highlight}
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2 rounded-lg bg-surface-container-low p-4">
          <div className="flex items-center gap-2">
            <GraduationCap
              aria-hidden="true"
              className="size-4 text-on-surface-variant"
            />
            <h3 className="text-title-md font-semibold text-primary-950">
              Formação Acadêmica &amp; Certificações Globais
            </h3>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="inline-flex items-center gap-1.5 rounded bg-card px-3 py-1.5 text-label-sm font-semibold text-primary-container">
              <GraduationCap aria-hidden="true" className="size-3.5" />
              {facts.education}
            </span>
            {facts.certifications.map((certification) => (
              <span
                key={certification}
                className="inline-flex items-center gap-1.5 rounded bg-card px-3 py-1.5 text-label-sm font-semibold text-primary-container"
              >
                <Award aria-hidden="true" className="size-3.5" />
                {certification}
              </span>
            ))}
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
