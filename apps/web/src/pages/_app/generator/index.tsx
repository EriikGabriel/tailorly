import { ValidateDialog } from "@components/quick-review/validate-dialog";
import { useBaseCvStore } from "@stores/base-cv-store";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { AiGuidelinesCard } from "./-components/ai-guidelines-card";
import { DocumentStage } from "./-components/document-stage";
import { GenerateCta } from "./-components/generate-cta";
import { GroundTruthCard } from "./-components/ground-truth-card";
import { JobTargetCard } from "./-components/job-target-card";
import { SecondBrainCard } from "./-components/second-brain-card";
import { SupplementaryEvidenceCard } from "./-components/supplementary-evidence-card";
import { WorkspaceHeader } from "./-components/workspace-header";
import type { ViewMode } from "./-types/view-mode";

export const Route = createFileRoute("/_app/generator/")({
  component: GeneratorPage,
});

function GeneratorPage() {
  const baseCvFile = useBaseCvStore((state) => state.file);
  const [viewMode, setViewMode] = useState<ViewMode>("visual");
  const splitMode = viewMode === "split";

  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <section
        aria-labelledby="generator-heading"
        className="mx-auto flex w-full max-w-384 flex-col gap-4 rounded-2xl p-4 shadow-xs sm:p-6"
      >
        <WorkspaceHeader baseCvFile={baseCvFile} />

        <div className="grid grid-cols-1 gap-6 pt-1 xl:grid-cols-12 xl:items-stretch">
          {!splitMode && (
            <div className="flex flex-col gap-4 rounded-xl bg-card p-6 shadow-md xl:col-span-5">
              <GroundTruthCard />
              <JobTargetCard />
              <AiGuidelinesCard />
              <SupplementaryEvidenceCard />
              <SecondBrainCard />
              <GenerateCta />
            </div>
          )}

          <div
            className={
              splitMode
                ? "flex h-full flex-col xl:col-span-12"
                : "flex h-full flex-col xl:col-span-7"
            }
          >
            <DocumentStage viewMode={viewMode} onViewModeChange={setViewMode} />
          </div>
        </div>
      </section>

      <ValidateDialog />
    </main>
  );
}
