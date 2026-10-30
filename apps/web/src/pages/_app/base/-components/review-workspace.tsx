import type { ReviewStepId } from "@@types/review-model";
import { useReviewIssues } from "@hooks/use-review-issues";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { forwardRef, useImperativeHandle, useRef } from "react";
import { firstStepWithPending, pendingByStep } from "@/utils/review-step-map";
import { clearSteps } from "@/utils/review-verification";
import { STEP_CATEGORIES } from "../-constants/evidence";
import { useApproveGroundTruthFlow } from "../-hooks/use-approve-ground-truth";
import { useConfirmReviewIssue } from "../-hooks/use-confirm-review-issue";
import { useReviewDraftHydration } from "../-hooks/use-review-draft-hydration";
import { useSaveReviewDraft } from "../-hooks/use-save-review-draft";
import { focusedRecordKeysFor } from "../-utils/focused-records";
import { countBlockingIssues } from "../-utils/review-draft-persistence";
import { MasterDocumentPanel } from "./master-document-panel";
import { PendingFieldsProvider } from "./pending-fields-provider";
import { ReviewCompletionCard } from "./review-completion-card";
import { ReviewStepNavigation } from "./review-step-navigation";
import { ReviewStepPanel } from "./review-step-panel";
import { ReviewStepper } from "./review-stepper";

export type ReviewWorkspaceHandle = {
  approve: () => void;
};

/**
 * Área de revisão do Currículo Base: documento original à esquerda e formulário por etapa à
 * direita. Só compõe os blocos; salvar, aprovar e confirmar pendências vivem nos hooks.
 */
export const ReviewWorkspace = forwardRef<
  ReviewWorkspaceHandle,
  { onSelectFile: () => void }
>(function ReviewWorkspace({ onSelectFile }, ref) {
  const { sourceId, activeStep, setActiveStep, expandedRecords } =
    useReviewDraftStore();
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const { review, issues: pendingIssues } = useReviewIssues();
  const records = review.records.data ?? [];

  useReviewDraftHydration(sourceId, review.records);

  const pending = pendingByStep(pendingIssues);
  const clearByStep = clearSteps(
    records,
    Object.fromEntries(
      Object.entries(pending).map(([step, value]) => [step, value.total]),
    ) as Record<ReviewStepId, number>,
    STEP_CATEGORIES,
  );

  const confirm = useConfirmReviewIssue(baseVersionId ?? "");
  const draftSave = useSaveReviewDraft(review);
  const approval = useApproveGroundTruthFlow({
    review,
    saveDraft: draftSave.save,
    onBlockingIssues: reviewPendingIssues,
  });

  // O botão do cabeçalho da página aprova por aqui, sempre com a versão mais recente do fluxo.
  const approveRef = useRef<() => void>(() => {});
  approveRef.current = approval.approve;
  useImperativeHandle(ref, () => ({ approve: () => approveRef.current() }), []);

  // Resolve aqui mesmo: abre a etapa com as pendências mais importantes para a vaga.
  function reviewPendingIssues() {
    const step = firstStepWithPending(pending);
    if (step) setActiveStep(step);
    document
      .getElementById("review-active-panel")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <PendingFieldsProvider
      issues={pendingIssues}
      records={records}
      clearSteps={clearByStep}
      onConfirm={confirm.confirmIssue}
      busy={confirm.busy}
    >
      <ReviewStepper
        activeStep={activeStep}
        onStepChange={setActiveStep}
        pending={pending}
        clear={clearByStep}
      />
      <div className="mx-auto mt-6 grid w-full max-w-384 items-start gap-6 xl:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <MasterDocumentPanel
          onSelectFile={onSelectFile}
          extractedPages={review.pages.data ?? []}
          extractionLoading={review.pages.isLoading}
          records={records}
          activeStep={activeStep}
          focusedRecordKeys={focusedRecordKeysFor(activeStep, expandedRecords)}
        />
        <div className="min-w-0 space-y-4">
          <ReviewStepPanel step={activeStep} />
          {confirm.error && (
            <p role="alert" className="text-sm text-error">
              {confirm.error}
            </p>
          )}
          <ReviewStepNavigation
            activeStep={activeStep}
            onStepChange={setActiveStep}
          />
          {activeStep === "skills" && (
            <ReviewCompletionCard
              onSave={draftSave.save}
              onApprove={approval.approve}
              onReviewIssues={reviewPendingIssues}
              saving={draftSave.saving}
              approving={approval.approving}
              disabled={
                !baseVersionId || review.records.isLoading || draftSave.saving
              }
              error={draftSave.error ?? approval.error}
              pendingCount={countBlockingIssues(review.issues.data ?? [])}
            />
          )}
        </div>
      </div>
    </PendingFieldsProvider>
  );
});
