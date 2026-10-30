import {
  Dialog,
  DialogBackdrop,
  DialogPopup,
  DialogPortal,
} from "@components/ui/animate/primitives/base/dialog";
import { useQuickReview } from "@hooks/use-quick-review";
import { IconExclamationMark } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import type { ReviewCategory, ReviewIssue } from "@/types/review";
import {
  fullReviewNotice,
  otherPendingNotice,
  quickReviewEmptyMessage,
  quickReviewListLabel,
  unprioritizedNotice,
} from "@/utils/quick-review-copy";
import { DialogFooter } from "./dialog-footer";
import { DialogHeader } from "./dialog-header";
import { JobRequirementsPanel } from "./job-requirements-panel";
import { ReviewNotice } from "./review-notice";
import { ReviewSection } from "./review-section";

type ValidateDialogProps = {
  issues?: readonly ReviewIssue[];
  requiresFullReview?: boolean;
};

/**
 * Diálogo de validação rápida antes de gerar o currículo: mostra as pendências (priorizadas pela
 * vaga quando possível) e deixa confirmar tudo ou responder uma a uma. O estado e as ações vivem
 * em `useQuickReview`; aqui só se monta a apresentação.
 */
export function ValidateDialog({
  issues,
  requiresFullReview = false,
}: ValidateDialogProps = {}) {
  const file = useBaseCvStore((state) => state.file);
  const review = useQuickReview({ issues, requiresFullReview });
  const { quickIssues, prioritized, otherPending, needsFullReview } = review;
  const count = quickIssues.length;
  const categories = [...new Set(quickIssues.map((issue) => issue.category))];
  const issuesLoading = review.review.issues.isLoading;
  const close = () => review.setOpen(false);

  return (
    <Dialog open={review.open} onOpenChange={review.setOpen}>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65 backdrop-blur-sm" />
        <DialogPopup
          className="flex max-h-[min(90dvh,760px)] w-[min(800px,calc(100vw-2rem))] max-w-none flex-col gap-0 overflow-hidden rounded-xl border border-primary-200 bg-card p-0 text-primary-950 shadow-2xl"
          style={{
            position: "fixed",
            top: "50%",
            left: "50%",
            translate: "-50% -50%",
            zIndex: 51,
          }}
        >
          <DialogHeader
            count={count}
            prioritized={prioritized}
            onClose={close}
          />

          <div className="min-h-0 space-y-4 overflow-y-auto overscroll-contain p-5 sm:px-7">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/30 pb-1.5">
              <p className="flex items-center text-xs font-bold uppercase tracking-wide text-secondary-700">
                <IconExclamationMark className="size-4.5 stroke-3 text-tertiary" />
                {quickReviewListLabel(count, prioritized)}
              </p>
              {file && (
                <p
                  className="max-w-full py-1 truncate text-xs"
                  title={file.name}
                >
                  Currículo Base:{" "}
                  <span className="font-semibold">{file.name}</span>
                </p>
              )}
            </div>

            {prioritized && review.jobMatch && (
              <JobRequirementsPanel
                title={review.jobVersion?.details?.title}
                company={review.jobVersion?.details?.company?.name}
                matches={review.verifiable}
              />
            )}
            {!prioritized &&
              review.jobVersion &&
              !issues &&
              review.review.records.data && (
                <ReviewNotice>
                  {unprioritizedNotice(
                    review.jobVersion.analysisStatus === "completed",
                  )}
                </ReviewNotice>
              )}
            {otherPending > 0 && (
              <ReviewNotice>{otherPendingNotice(otherPending)}</ReviewNotice>
            )}
            {needsFullReview && (
              <ReviewNotice>
                {fullReviewNotice(count, review.allIssues.length)}
              </ReviewNotice>
            )}

            {categories.map((category: ReviewCategory) => (
              <ReviewSection
                key={category}
                category={category}
                issues={quickIssues.filter(
                  (issue) => issue.category === category,
                )}
                answers={review.answers}
                onAnswerChange={review.updateAnswer}
              />
            ))}
            {issuesLoading && <p role="status">Carregando pendências…</p>}
            {review.review.issues.isError && (
              <p role="alert">
                Não foi possível carregar as pendências desta versão.
              </p>
            )}
            {count === 0 && !needsFullReview && (
              <p className="text-sm text-on-surface-variant">
                {quickReviewEmptyMessage(
                  prioritized,
                  Boolean(review.baseVersionId),
                )}
              </p>
            )}
            {review.error && (
              <p role="alert" className="text-sm text-error">
                {review.error}
              </p>
            )}
          </div>

          <DialogFooter
            needsFullReview={needsFullReview}
            hasQuickIssues={count > 0}
            canConfirmAll={
              Boolean(review.baseVersionId) &&
              !issuesLoading &&
              !review.submitting
            }
            canFinish={
              review.reviewComplete &&
              Boolean(review.baseVersionId) &&
              !issuesLoading &&
              !review.submitting &&
              !review.generating
            }
            generating={review.generating}
            onClose={close}
            onConfirmAll={review.confirmAll}
            onFinish={review.finishReview}
            onGoToFullReview={review.goToFullReview}
          />
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}
