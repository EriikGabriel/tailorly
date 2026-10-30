import { ApiClientError } from "@lib/api/client";
import { apiErrorMessage } from "@lib/api/error-message";
import { useGenerateCv } from "@queries/cv-generation";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useQuickReviewStore } from "@stores/quick-review-store";
import { useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import type { ReviewAnswer, ReviewIssue } from "@/types/review";
import {
  isReviewIssueComplete,
  selectQuickReviewIssues,
} from "@/utils/review-issues";
import { useReviewIssues, useSubmitReviewAnswers } from "./use-review-issues";

type UseQuickReviewInput = {
  /** Pendências fornecidas de fora (ex.: prévia); sem elas, usa as da API. */
  issues?: readonly ReviewIssue[];
  requiresFullReview: boolean;
};

/**
 * Estado e ações do diálogo de validação rápida: escolhe quais pendências mostrar (priorizadas
 * pela vaga ou todas), guarda as respostas por contexto, confirma tudo de uma vez e conclui a
 * revisão disparando a geração do currículo.
 */
export function useQuickReview({
  issues,
  requiresFullReview,
}: UseQuickReviewInput) {
  const navigate = useNavigate();
  const { selectionId, baseVersionId } = useBaseCvStore();
  const reviewIssues = useReviewIssues();
  const { issues: apiIssues, relevantIssues, canPrioritize } = reviewIssues;
  const submitAnswers = useSubmitReviewAnswers(baseVersionId ?? "");
  const generateCv = useGenerateCv();
  const { revision: jobRevision, saveDraft } = useJobDraftStore();
  const quickReview = useQuickReviewStore();
  const [error, setError] = useState<string | null>(null);

  const prioritized = !issues && canPrioritize;
  const otherPending = prioritized
    ? apiIssues.length - relevantIssues.length
    : 0;
  const allIssues = issues ?? (prioritized ? relevantIssues : apiIssues);

  // As respostas só valem para o mesmo contexto (arquivo, vaga, versão e perguntas).
  const contextKey = JSON.stringify([
    selectionId,
    jobRevision,
    baseVersionId,
    allIssues,
    requiresFullReview,
  ]);
  const { configure } = quickReview;
  useEffect(() => {
    configure(contextKey, allIssues, false);
  }, [configure, contextKey, allIssues]);

  const answers =
    quickReview.contextKey === contextKey ? quickReview.answers : {};
  const quickIssues =
    prioritized || apiIssues.length > 0
      ? allIssues
      : selectQuickReviewIssues(allIssues);
  const needsFullReview = requiresFullReview || quickIssues.length > 0;
  const reviewComplete = quickIssues.every((issue) =>
    isReviewIssueComplete(issue, answers[issue.id]),
  );

  function updateAnswer(id: string, answer: ReviewAnswer) {
    quickReview.setAnswer(contextKey, id, answer);
  }

  async function confirmAll() {
    if (!baseVersionId) {
      setError("Envie o Currículo Base antes de confirmar os dados.");
      return;
    }
    setError(null);
    try {
      await submitAnswers.submit(
        quickIssues.map((issue) => ({
          issue,
          answer: { choice: "confirmado" as const, correction: "" },
        })),
      );
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível confirmar os dados."));
    }
  }

  async function finishReview() {
    if (
      needsFullReview ||
      !reviewComplete ||
      quickReview.contextKey !== contextKey
    )
      return;
    if (!baseVersionId) {
      setError("Envie o Currículo Base antes de salvar as respostas.");
      return;
    }
    setError(null);
    try {
      await submitAnswers.submit(
        quickIssues.map((issue) => ({ issue, answer: answers[issue.id] })),
      );
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível salvar as respostas."));
      return;
    }
    saveDraft();
    // A esteira roda no servidor (IA de layout, compilação e nota de ATS) e leva alguns segundos.
    try {
      await generateCv.mutateAsync();
    } catch (cause) {
      if (cause instanceof ApiClientError && cause.status === 400) {
        quickReview.setOpen(false);
        navigate({ to: "/base" });
        return;
      }
      setError(apiErrorMessage(cause, "Não foi possível gerar o currículo."));
      return;
    }
    quickReview.setOpen(false);
    navigate({ to: "/generator" });
  }

  /** Fecha o diálogo guardando o rascunho da vaga, para seguir à revisão completa. */
  function goToFullReview() {
    saveDraft();
    quickReview.setOpen(false);
  }

  return {
    review: reviewIssues.review,
    jobMatch: reviewIssues.jobMatch,
    jobVersion: reviewIssues.jobVersion,
    verifiable: reviewIssues.verifiable,
    open: quickReview.open,
    setOpen: quickReview.setOpen,
    baseVersionId,
    answers,
    allIssues,
    quickIssues,
    prioritized,
    otherPending,
    needsFullReview,
    reviewComplete,
    updateAnswer,
    confirmAll,
    finishReview,
    goToFullReview,
    error,
    submitting: submitAnswers.isPending,
    generating: generateCv.isPending,
  };
}
