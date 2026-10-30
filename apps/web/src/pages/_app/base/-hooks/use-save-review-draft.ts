import type { FactRecord } from "@@types/base-review";
import { apiErrorMessage } from "@lib/api/error-message";
import {
  type BaseReviewQueries,
  useAnswerReviewIssue,
  useCreateFact,
  useCreateFactRecord,
  useInvalidateBaseReview,
  useUpdateFactReviewState,
} from "@queries/base-review";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { useState } from "react";
import {
  buildFactGroups,
  type FactGroup,
  openIssueIdByFact,
} from "../-utils/review-draft-persistence";

/**
 * Grava o rascunho de revisão na API: cria registros e fatos novos, corrige os alterados e omite
 * os removidos. Quando o fato tem pendência aberta, a decisão é registrada pela resposta dela.
 */
export function useSaveReviewDraft(review: BaseReviewQueries) {
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const draft = useReviewDraftStore((state) => state.draft);
  const versionId = baseVersionId ?? "";
  const createRecord = useCreateFactRecord(versionId);
  const createFact = useCreateFact(versionId);
  const updateFact = useUpdateFactReviewState(versionId);
  const answerIssue = useAnswerReviewIssue(versionId);
  const invalidateReview = useInvalidateBaseReview(versionId);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function syncFactGroup(
    group: FactGroup,
    displayOrder: number,
    openIssues: Map<string, string>,
  ) {
    let record: FactRecord | undefined = review.records.data?.find(
      (item) => item.category === group.category,
    );
    if (!record) {
      record = await createRecord.mutateAsync({
        baseVersionId: versionId,
        category: group.category,
        displayOrder,
      });
    }
    const existing = new Map(record.facts.map((fact) => [fact.fieldKey, fact]));

    for (const [fieldKey, value] of Object.entries(group.fields)) {
      const fact = existing.get(fieldKey);
      if (!fact) {
        await createFact.mutateAsync({
          recordId: record.id,
          fieldKey,
          reviewedValue: value,
          origin: "manual_entry",
          reviewState: "pendente",
        });
        continue;
      }
      const currentValue = String(
        fact.reviewedValue ?? fact.extractedValue ?? "",
      );
      if (currentValue === value) continue;
      const issueId = openIssues.get(fact.id);
      if (issueId) {
        await answerIssue.mutateAsync({
          id: issueId,
          answer: { reviewState: "corrigido", reviewedValue: value },
        });
      } else {
        await updateFact.mutateAsync({
          id: fact.id,
          reviewState: "corrigido",
          reviewedValue: value,
        });
      }
    }

    for (const fact of record.facts) {
      if (fact.reviewState === "omitido" || fact.fieldKey in group.fields) {
        continue;
      }
      const issueId = openIssues.get(fact.id);
      if (issueId) {
        await answerIssue.mutateAsync({
          id: issueId,
          answer: { reviewState: "omitido" },
        });
      } else {
        await updateFact.mutateAsync({ id: fact.id, reviewState: "omitido" });
      }
    }
  }

  /** Devolve `true` quando tudo foi gravado; em falha, `error` traz o motivo. */
  async function save(): Promise<boolean> {
    if (!baseVersionId) {
      setError("Envie o Currículo Base antes de salvar os fatos.");
      return false;
    }
    if (review.records.isLoading) return false;
    setSaving(true);
    setError(null);
    try {
      const groups = buildFactGroups(draft, review.records.data ?? []);
      const openIssues = openIssueIdByFact(review.issues.data ?? []);
      for (const [index, group] of groups.entries()) {
        await syncFactGroup(group, index, openIssues);
      }
      invalidateReview();
      return true;
    } catch (cause) {
      invalidateReview();
      setError(apiErrorMessage(cause, "Não foi possível salvar os fatos."));
      return false;
    } finally {
      setSaving(false);
    }
  }

  return { save, saving, error };
}
