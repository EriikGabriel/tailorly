import { PageHeading } from "@components/page-heading";
import { ScanSearch } from "@react-zero-ui/icon-sprite";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { DueDiligenceForm } from "./-components/due-diligence-form";
import { DueDiligenceHistory } from "./-components/due-diligence-history";
import { DueDiligencePrinciples } from "./-components/due-diligence-principles";
import { DueDiligenceReportView } from "./-components/due-diligence-report-view";

export const Route = createFileRoute("/_app/due-diligence/")({
  component: DueDiligencePage,
});

/** Due diligence de empresas: pedir uma análise, reabrir as salvas e ler os onze fatores com suas fontes. */
function DueDiligencePage() {
  const [selectedId, setSelectedId] = useState<string | null>(null);

  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <section
        aria-labelledby="due-diligence-heading"
        className="mx-auto flex w-full max-w-384 flex-col gap-4 rounded-2xl p-4 shadow-xs sm:p-6"
      >
        <PageHeading
          headingId="due-diligence-heading"
          breadcrumb="Due Diligence"
          eyebrowIcon={ScanSearch}
          eyebrow="Apoio à decisão"
          title="Due Diligence de Empresas"
          description={
            <>
              Consolida sinais públicos sobre a empresa de uma vaga em onze
              fatores, cada um com{" "}
              <strong className="font-semibold text-primary-950">
                fonte, data, entidade analisada e incerteza
              </strong>
              . Suas análises ficam salvas na sua conta.
            </>
          }
        />

        <DueDiligencePrinciples />

        <div className="grid gap-6 xl:grid-cols-12">
          <div className="flex flex-col gap-4 xl:col-span-4">
            <DueDiligenceForm onCreated={setSelectedId} />
            <DueDiligenceHistory
              selectedId={selectedId}
              onSelect={setSelectedId}
            />
          </div>
          <div className="xl:col-span-8">
            <DueDiligenceReportView id={selectedId} />
          </div>
        </div>
      </section>
    </main>
  );
}
