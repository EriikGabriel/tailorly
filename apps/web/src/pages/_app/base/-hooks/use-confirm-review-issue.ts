import type { ReviewIssue } from "@@types/review";
import { apiErrorMessage } from "@api/error-message";
import { useSubmitReviewAnswers } from "@hooks/use-review-issues";
import { useState } from "react";

/** Confirma o valor extraído de uma pendência ("Está correto") direto do campo da revisão. */
export function useConfirmReviewIssue(baseVersionId: string) {
  const submitAnswers = useSubmitReviewAnswers(baseVersionId);
  const [error, setError] = useState<string | null>(null);

  async function confirmIssue(issue: ReviewIssue) {
    setError(null);
    try {
      await submitAnswers.submit([
        { issue, answer: { choice: "confirmado", correction: "" } },
      ]);
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível confirmar o valor."));
    }
  }

  return { confirmIssue, busy: submitAnswers.isPending, error };
}
