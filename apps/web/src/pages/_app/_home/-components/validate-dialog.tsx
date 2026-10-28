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
  useAnswerReviewIssue,
  useBaseReview,
  useInvalidateBaseReview,
} from "@queries/base-review";
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
import { useEffect, useMemo, useState } from "react";
import type {
  ReviewAnswer,
  ReviewCategory,
  ReviewChoice,
  ReviewIssue,
} from "@/types/review";
import { factFieldLabel, formatFactValue } from "@/utils/fact-display";
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
  const { file, selectionId, baseVersionId } = useBaseCvStore();
  const review = useBaseReview(baseVersionId ?? undefined);
  const answerIssue = useAnswerReviewIssue(baseVersionId ?? "");
  const invalidateReview = useInvalidateBaseReview(baseVersionId ?? "");
  const { revision: jobRevision, saveDraft: onSaveDraft } = useJobDraftStore();
  const {
    open,
    setOpen: onOpenChange,
    contextKey: storedContext,
    answers: storedAnswers,
    configure,
    setAnswer,
  } = useQuickReviewStore();
  const [error, setError] = useState<string | null>(null);
  const apiIssues = useMemo(() => {
    // Categorias dos fatos (backend) -> seções da revisão rápida.
    const reviewCategoryByFactCategory: Record<string, ReviewCategory> = {
      identity: "identity",
      experience: "employment",
      volunteering: "volunteering",
      projects: "projects",
      degrees: "education",
      certifications: "education",
      languages: "languages",
      technologies: "skills",
    };
    return (review.issues.data ?? [])
      .filter((issue) => issue.status === "PENDING" && issue.fact)
      .map((issue): ReviewIssue => {
        const configured = issue.questionAndChoices ?? {};
        const fieldLabel = factFieldLabel(
          issue.fact?.fieldKey ?? "",
          issue.category,
        );
        const currentValue = formatFactValue(
          issue.fact?.reviewedValue ?? issue.fact?.extractedValue,
        );
        const rawChoices = Array.isArray(configured.choices)
          ? configured.choices
          : ["confirmado", "corrigido", "omitido"];
        const choices: ReviewChoice[] = rawChoices.flatMap(
          (raw): ReviewChoice[] => {
            if (raw === "confirmado")
              return [
                { value: raw, label: "Está correto", result: "confirmed" },
              ];
            if (raw === "corrigido")
              return [
                {
                  value: raw,
                  label: "Corrigir",
                  result: "corrected",
                  correction: {
                    label: `Valor correto para ${fieldLabel}`,
                    type: "text",
                    placeholder: currentValue,
                    originalValue: currentValue,
                  },
                },
              ];
            if (raw === "omitido")
              return [
                {
                  value: raw,
                  label: "Não usar no currículo",
                  result: "omitted",
                },
              ];
            return [];
          },
        );
        return {
          id: issue.id,
          category: reviewCategoryByFactCategory[issue.category] ?? "identity",
          type: "fact",
          question: `${fieldLabel} está correto?`,
          fieldLabel,
          value: currentValue,
          evidence: issue.fact?.evidence[0]?.sourceExcerpt,
          motivation: issue.reason,
          choices,
        };
      });
  }, [review.issues.data]);
  const allIssues = issues ?? apiIssues;
  const contextKey = JSON.stringify([
    selectionId,
    jobRevision,
    baseVersionId,
    allIssues,
    requiresFullReview,
  ]);
  const answers = storedContext === contextKey ? storedAnswers : {};

  useEffect(() => {
    configure(contextKey, allIssues, false);
  }, [configure, contextKey, allIssues]);

  const quickIssues =
    apiIssues.length > 0 ? apiIssues : selectQuickReviewIssues(allIssues);
  const needsFullReview =
    requiresFullReview || quickIssues.length < allIssues.length;
  const reviewComplete = quickIssues.every((issue) =>
    isReviewIssueComplete(issue, answers[issue.id]),
  );
  const categories = [...new Set(quickIssues.map((issue) => issue.category))];

  function updateAnswer(id: string, answer: ReviewAnswer) {
    setAnswer(contextKey, id, answer);
  }

  async function finishReview() {
    if (needsFullReview || !reviewComplete || storedContext !== contextKey)
      return;
    if (!baseVersionId) {
      setError("Envie o Currículo Base antes de salvar as respostas.");
      return;
    }
    setError(null);
    try {
      for (const issue of quickIssues) {
        const answer = answers[issue.id];
        const choice = issue.choices.find(
          (item) => item.value === answer.choice,
        );
        if (!choice) continue;
        const reviewState =
          choice.result === "confirmed"
            ? "confirmado"
            : choice.result === "corrected"
              ? "corrigido"
              : "omitido";
        await answerIssue.mutateAsync({
          id: issue.id,
          answer: {
            reviewState,
            ...(choice.result === "corrected"
              ? { reviewedValue: answer.correction }
              : {}),
          },
        });
      }
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Não foi possível salvar as respostas.",
      );
      return;
    } finally {
      invalidateReview();
    }
    onSaveDraft();
    onOpenChange(false);
    navigate({ to: "/generator" });
  }

  const count = quickIssues.length;
  const heading =
    count === 0
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
                  {`${count} pendências para revisar`}
                </span>
                <DialogDescription className="text-sm leading-5 text-on-surface-variant">
                  "Pendências persistidas na versão candidata do Currículo Base.
                  As respostas são registradas na API."
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
            {review.issues.isLoading && (
              <p role="status">Carregando pendências…</p>
            )}
            {review.issues.isError && (
              <p role="alert">
                Não foi possível carregar as pendências desta versão.
              </p>
            )}
            {count === 0 && !needsFullReview && (
              <p className="text-sm text-on-surface-variant">
                {baseVersionId
                  ? "Não há pendências reais nesta versão candidata."
                  : "Envie o Currículo Base para carregar pendências reais."}
              </p>
            )}
            {error && (
              <p role="alert" className="text-sm text-error">
                {error}
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
                  disabled={
                    !reviewComplete ||
                    !baseVersionId ||
                    review.issues.isLoading ||
                    answerIssue.isPending
                  }
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
