import { Button } from "@components/ui/animate/buttons/button";
import {
  Dialog,
  DialogBackdrop,
  DialogDescription,
  DialogPopup,
  DialogPortal,
  DialogTitle,
} from "@components/ui/animate/primitives/base/dialog";
import {
  ArrowRight,
  IconExclamationMark,
  ListTodo,
  Sparkles,
  X,
} from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useQuickReviewStore } from "@stores/quick-review-store";
import { Link, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { previewReviewIssues } from "@/data/review-preview";
import type { ReviewAnswer, ReviewCategory, ReviewIssue } from "@/types/review";
import {
  isReviewIssueComplete,
  selectQuickReviewIssues,
} from "@/utils/review-issues";
import { ReviewSection } from "./review-section";

type ValidateDialogProps = {
  issues?: readonly ReviewIssue[];
  requiresFullReview?: boolean;
};

export function ValidateDialog({
  issues,
  requiresFullReview = false,
}: ValidateDialogProps = {}) {
  const navigate = useNavigate();
  const file = useBaseCvStore((state) => state.file);
  const selectionId = useBaseCvStore((state) => state.selectionId);
  const jobRevision = useJobDraftStore((state) => state.revision);
  const onSaveDraft = useJobDraftStore((state) => state.saveDraft);
  const open = useQuickReviewStore((state) => state.open);
  const onOpenChange = useQuickReviewStore((state) => state.setOpen);
  const storedContext = useQuickReviewStore((state) => state.contextKey);
  const storedAnswers = useQuickReviewStore((state) => state.answers);
  const configure = useQuickReviewStore((state) => state.configure);
  const setAnswer = useQuickReviewStore((state) => state.setAnswer);
  const savePreview = useQuickReviewStore((state) => state.savePreview);
  const preview = issues === undefined;
  const allIssues = issues ?? previewReviewIssues;
  const contextKey = JSON.stringify([
    selectionId,
    jobRevision,
    preview,
    allIssues,
    requiresFullReview,
  ]);
  const answers = storedContext === contextKey ? storedAnswers : {};

  useEffect(() => {
    configure(contextKey, allIssues, preview);
  }, [configure, contextKey, allIssues, preview]);

  const quickIssues = selectQuickReviewIssues(allIssues);
  const needsFullReview =
    requiresFullReview || quickIssues.length < allIssues.length;
  const reviewComplete = quickIssues.every((issue) =>
    isReviewIssueComplete(issue, answers[issue.id]),
  );
  const categories = [...new Set(quickIssues.map((issue) => issue.category))];

  function updateAnswer(id: string, answer: ReviewAnswer) {
    setAnswer(contextKey, id, answer);
  }

  function finishReview() {
    if (needsFullReview || !reviewComplete || storedContext !== contextKey)
      return;
    onSaveDraft();
    savePreview(contextKey);
    onOpenChange(false);
    navigate({ to: "/generator" });
  }

  const count = quickIssues.length;
  const heading = preview
    ? "Prévia da revisão de currículo"
    : count === 0
      ? "Tudo pronto para gerar"
      : `Confirme ${count} ${count === 1 ? "informação" : "informações"} antes de gerar`;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65 backdrop-blur-sm" />
        <DialogPopup
          className="flex max-h-[min(90dvh,760px)] w-[min(680px,calc(100vw-2rem))] max-w-none flex-col gap-0 overflow-hidden rounded-xl border border-primary-200 bg-card p-0 text-primary-950 shadow-2xl"
          style={{
            position: "fixed",
            top: "50%",
            left: "50%",
            translate: "-50% -50%",
            zIndex: 51,
          }}
        >
          <div className="shrink-0 border-b border-outline-variant/30 bg-surface-container-low px-5 py-5 sm:px-7">
            <div className="flex items-start justify-between gap-3">
              <div className="flex flex-col gap-2">
                <DialogTitle className="flex items-center gap-2 text-lg font-semibold leading-6">
                  <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-white">
                    <ListTodo className="size-4.5" />
                  </span>
                  {heading}
                </DialogTitle>
                <span className="w-fit rounded-full border bg-white px-3 py-0.5 text-[10px] font-bold uppercase leading-4 tracking-[0.3px] text-tertiary">
                  {preview
                    ? `${count} exemplos de revisão`
                    : `${count} dúvidas para revisar`}
                </span>
                <DialogDescription className="text-sm leading-5 text-on-surface-variant">
                  {preview
                    ? "Demonstração com dados fictícios, não extraídos do seu arquivo. As respostas ficam apenas como rascunho de interface."
                    : "Revise as pendências desta seleção antes de continuar."}
                </DialogDescription>
              </div>
              <Button
                variant="ghost"
                type="button"
                onClick={() => onOpenChange(false)}
                aria-label="Fechar validação"
                className="rounded-md p-2 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
              >
                <X className="size-4" />
              </Button>
            </div>
          </div>

          <div className="min-h-0 space-y-4 overflow-y-auto overscroll-contain px-5 py-5 sm:px-7">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/30 pb-1.5">
              <p className="flex items-center text-xs font-bold uppercase tracking-wide text-secondary-700">
                <IconExclamationMark className="size-4.5 stroke-3 text-tertiary" />
                {count === 0
                  ? "Nenhuma pendência"
                  : "Itens que exigem sua confirmação"}
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

            {needsFullReview && (
              <p className="rounded-lg border border-outline-variant/40 bg-surface-container-low p-3 text-sm text-on-surface-variant">
                {allIssues.length > count
                  ? `Mostrando ${count} de ${allIssues.length} pendências. `
                  : "Este currículo exige revisão de mais detalhes. "}
                Continue na revisão completa antes de gerar.
              </p>
            )}

            {categories.map((category: ReviewCategory) => (
              <ReviewSection
                key={category}
                category={category}
                issues={quickIssues.filter(
                  (issue) => issue.category === category,
                )}
                answers={answers}
                onAnswerChange={updateAnswer}
              />
            ))}
            {count === 0 && !needsFullReview && (
              <p className="text-sm text-on-surface-variant">
                Não há informações a confirmar nesta versão.
              </p>
            )}
          </div>

          <div className="flex shrink-0 flex-col gap-3 bg-surface-container-low border-t border-outline-variant/30 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7">
            <Link
              to="/base"
              onClick={() => {
                onSaveDraft();
                onOpenChange(false);
              }}
              className="text-xs font-semibold text-secondary-700 underline underline-offset-2 hover:text-primary-900"
            >
              Revisar currículo completo
            </Link>
            <div className="flex flex-wrap gap-4">
              <Button
                variant="outline"
                type="button"
                onClick={() => onOpenChange(false)}
                className="rounded-xl border border-outline-variant/50 px-6 py-6 text-sm font-medium hover:bg-surface-container"
                disableZoom
              >
                Agora não
              </Button>
              {needsFullReview ? (
                <Link
                  to="/base"
                  onClick={() => {
                    onSaveDraft();
                    onOpenChange(false);
                  }}
                  className="rounded-xl bg-primary-900 px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary-800"
                >
                  Ir para revisão completa
                </Link>
              ) : (
                <Button
                  type="button"
                  onClick={finishReview}
                  disabled={!reviewComplete}
                  className="rounded-xl bg-primary-900 px-6! py-6 text-sm font-semibold text-primary-foreground hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-40"
                  disableZoom
                >
                  <Sparkles
                    strokeWidth={1.75}
                    className="size-5 shrink-0"
                    aria-hidden="true"
                  />
                  Continuar para prévia
                  <ArrowRight
                    strokeWidth={1.75}
                    className="size-5 shrink-0"
                    aria-hidden="true"
                  />
                </Button>
              )}
            </div>
          </div>
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}
