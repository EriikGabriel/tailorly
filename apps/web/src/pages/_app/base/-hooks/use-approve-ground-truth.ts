import { apiErrorMessage } from "@lib/api/error-message";
import {
  type BaseReviewQueries,
  useApproveGroundTruth,
} from "@queries/base-review";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { hasJobTarget } from "@/utils/job-draft";
import { hasBlockingIssues } from "../-utils/review-draft-persistence";

type UseApproveGroundTruthInput = {
  review: BaseReviewQueries;
  /** Grava o rascunho antes de aprovar; devolve `false` se não foi possível. */
  saveDraft: () => Promise<boolean>;
  /** Chamado quando ainda há pendências bloqueantes, para levar a pessoa até elas. */
  onBlockingIssues: () => void;
};

/**
 * Fluxo de aprovação do Ground Truth: salva o rascunho, confere se restam pendências bloqueantes
 * e aprova o snapshot. Com uma vaga em andamento, segue para o gerador em vez de voltar ao início.
 */
export function useApproveGroundTruthFlow({
  review,
  saveDraft,
  onBlockingIssues,
}: UseApproveGroundTruthInput) {
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const { mode, jobUrl, jobText } = useJobDraftStore();
  const approveGroundTruth = useApproveGroundTruth(baseVersionId ?? "");
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  async function approve() {
    if (!(await saveDraft())) return;
    const refreshed = await review.issues.refetch();
    if (hasBlockingIssues(refreshed.data ?? [])) {
      onBlockingIssues();
      return;
    }
    setError(null);
    try {
      await approveGroundTruth.mutateAsync();
      navigate({
        to: hasJobTarget(mode, jobUrl, jobText) ? "/generator" : "/",
      });
    } catch (cause) {
      setError(
        apiErrorMessage(cause, "Não foi possível aprovar o Ground Truth."),
      );
    }
  }

  return { approve, approving: approveGroundTruth.isPending, error };
}
