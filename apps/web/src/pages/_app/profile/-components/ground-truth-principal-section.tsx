import { useActiveGroundTruth } from "@queries/ground-truth";
import {
  ArrowRight,
  Award,
  Briefcase,
  ClipboardCheck,
  GraduationCap,
} from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import { formatPeriod, isCurrentStatus } from "@/utils/review-format";
import { SectionCard } from "./section-card";

export function GroundTruthPrincipalSection() {
  const { isLoading, hasActiveSnapshot, facts } = useActiveGroundTruth();

  return (
    <SectionCard
      icon={ClipboardCheck}
      title="2. Ground Truth Principal (Verdade Absoluta)"
      description="Fatos aprovados no Currículo Base. A IA é restrita a usar apenas dados validados nesta curadoria."
      trailing={
        <Link
          to="/base"
          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-label-md font-semibold text-primary-foreground hover:bg-primary/90"
        >
          Curar &amp; Auditar Dossiê de Fatos
          <ArrowRight aria-hidden="true" className="size-3.5" />
        </Link>
      }
    >
      {isLoading ? (
        <p className="text-body-sm text-on-surface-variant">Carregando...</p>
      ) : !hasActiveSnapshot || !facts ? (
        <p className="rounded-lg bg-surface-container-low p-4 text-body-sm text-on-surface-variant">
          Sem Ground Truth ativo. Envie e aprove um Currículo Base para ver seus
          fatos aqui.
        </p>
      ) : (
        <div className="flex flex-col gap-4">
          {facts.identity.summary && (
            <div className="flex flex-col gap-2 rounded-lg bg-surface-container-low p-4">
              <div className="flex items-center gap-2">
                <ClipboardCheck
                  aria-hidden="true"
                  className="size-4 text-on-surface-variant"
                />
                <h3 className="text-title-md font-semibold text-primary-950">
                  Resumo Executivo Base
                </h3>
              </div>
              <p className="text-body-sm text-on-surface-variant">
                &quot;{facts.identity.summary}&quot;
              </p>
            </div>
          )}

          {facts.experience.length > 0 && (
            <div className="flex flex-col gap-3 rounded-lg bg-surface-container-low p-4">
              <div className="flex items-center gap-2">
                <Briefcase
                  aria-hidden="true"
                  className="size-4 text-on-surface-variant"
                />
                <h3 className="text-title-md font-semibold text-primary-950">
                  Histórico Profissional Mapeado ({facts.experience.length}{" "}
                  Posições)
                </h3>
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                {facts.experience.map((position, index) => (
                  <div
                    // biome-ignore lint/suspicious/noArrayIndexKey: fatos não têm identificador próprio aqui, só a ordem aprovada.
                    key={index}
                    className="flex flex-col gap-1 rounded-lg bg-card p-3"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-label-md font-semibold text-primary-950">
                        {position.role ?? "—"}
                      </span>
                      <span className="shrink-0 text-label-sm font-medium text-on-secondary-container">
                        {formatPeriod(
                          position.start,
                          position.end,
                          isCurrentStatus(position.status),
                        )}
                      </span>
                    </div>
                    <p className="text-body-sm text-on-surface-variant">
                      {position.company}
                    </p>
                    {position.description && (
                      <p className="text-label-sm font-medium text-on-surface-variant">
                        {position.description}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {(facts.degrees.length > 0 || facts.certifications.length > 0) && (
            <div className="flex flex-col gap-2 rounded-lg bg-surface-container-low p-4">
              <div className="flex items-center gap-2">
                <GraduationCap
                  aria-hidden="true"
                  className="size-4 text-on-surface-variant"
                />
                <h3 className="text-title-md font-semibold text-primary-950">
                  Formação Acadêmica &amp; Certificações
                </h3>
              </div>
              <div className="flex flex-wrap gap-2">
                {facts.degrees.map((degree, index) => (
                  <span
                    // biome-ignore lint/suspicious/noArrayIndexKey: fatos não têm identificador próprio aqui, só a ordem aprovada.
                    key={index}
                    className="inline-flex items-center gap-1.5 rounded bg-card px-3 py-1.5 text-label-sm font-semibold text-primary-container"
                  >
                    <GraduationCap aria-hidden="true" className="size-3.5" />
                    {degree.degree}
                    {degree.institution ? ` — ${degree.institution}` : ""}
                  </span>
                ))}
                {facts.certifications.map((certification, index) => (
                  <span
                    // biome-ignore lint/suspicious/noArrayIndexKey: fatos não têm identificador próprio aqui, só a ordem aprovada.
                    key={index}
                    className="inline-flex items-center gap-1.5 rounded bg-card px-3 py-1.5 text-label-sm font-semibold text-primary-container"
                  >
                    <Award aria-hidden="true" className="size-3.5" />
                    {certification.name}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </SectionCard>
  );
}
