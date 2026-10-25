import { apiRequest } from "@lib/api/client";
import {
  extractedPageFindAllByRunId,
  extractedPageReadText,
  extractionRunFindByVersion,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export type FactEvidence = {
  id: string;
  extractedPageId: string;
  sourceExcerpt: string;
  sourceLocator: unknown;
};

export type BaseFact = {
  id: string;
  recordId: string;
  fieldKey: string;
  extractedValue: unknown;
  reviewedValue: unknown;
  origin: string;
  reviewState: "pendente" | "confirmado" | "corrigido" | "omitido";
  evidence: FactEvidence[];
};

export type FactRecord = {
  id: string;
  baseVersionId: string;
  category: string;
  displayOrder: number;
  facts: BaseFact[];
};

export type ReviewIssue = {
  id: string;
  baseVersionId: string;
  factId: string | null;
  category: string;
  reason: string;
  blocking: boolean;
  status: string;
  questionAndChoices: Record<string, unknown> | null;
  fact: BaseFact | null;
};

export type GroundTruthSnapshot = {
  id: string;
  ownerId: string;
  baseVersionId: string;
  snapshotNumber: number;
  approvedReviewRevision: number;
  contentHash: string;
  approvedAt: string;
  schemaVersion: number;
  active: boolean;
  items: {
    id: string;
    category: string;
    fieldKey: string;
    approvedValue: unknown;
  }[];
};

// Centraliza as mutações da revisão para invalidar os dados exibidos na tela.
function useBaseReviewMutation<TInput, TOutput>(
  versionId: string,
  path: (input: TInput) => string,
  method: "POST" | "PATCH",
  requestBody: (input: TInput) => unknown = (input) => input,
) {
  const accessToken = useAuthStore((state) => state.accessToken);
  const queryClient = useQueryClient();

  return useMutation<TOutput, Error, TInput>({
    mutationFn: (input) =>
      apiRequest<TOutput>(path(input), {
        method,
        accessToken: accessToken ?? undefined,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody(input)),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.records(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.issues(versionId),
      });
    },
  });
}

export function useBaseReview(versionId?: string) {
  const accessToken = useAuthStore((state) => state.accessToken);
  const runs = useQuery({
    queryKey: apiQueryKeys.extractionRuns.byVersion(versionId ?? ""),
    queryFn: () =>
      extractionRunFindByVersion(versionId ?? "", {
        accessToken: accessToken ?? undefined,
      }),
    enabled: Boolean(accessToken && versionId),
    refetchInterval: (query) =>
      query.state.data?.[0] &&
      ["QUEUED", "PROCESSING"].includes(query.state.data[0].status)
        ? 1500
        : false,
  });
  const completedRun = runs.data?.find((run) => run.status === "COMPLETED");
  const latestRun = runs.data?.[0];

  function refetchPendingInterpretation<T extends { length: number }>(query: {
    state: { data?: T; dataUpdatedAt: number };
  }) {
    if (latestRun && ["QUEUED", "PROCESSING"].includes(latestRun.status)) {
      return 1500;
    }

    if (
      latestRun?.status === "COMPLETED" &&
      query.state.data?.length === 0 &&
      latestRun.finishedAt &&
      Date.now() - Date.parse(latestRun.finishedAt) < 60_000
    ) {
      return 1500;
    }

    return false;
  }

  const pages = useQuery({
    queryKey: apiQueryKeys.baseReview.pages(completedRun?.id ?? ""),
    queryFn: () =>
      extractedPageFindAllByRunId(completedRun?.id ?? "", {
        accessToken: accessToken ?? undefined,
      }),
    enabled: Boolean(accessToken && completedRun),
  });
  const records = useQuery({
    queryKey: apiQueryKeys.baseReview.records(versionId ?? ""),
    queryFn: () =>
      apiRequest<FactRecord[]>(
        `/v1/fact-records/version/${encodeURIComponent(versionId ?? "")}`,
        { accessToken: accessToken ?? undefined },
      ),
    enabled: Boolean(accessToken && versionId),
    refetchInterval: refetchPendingInterpretation,
  });
  const issues = useQuery({
    queryKey: apiQueryKeys.baseReview.issues(versionId ?? ""),
    queryFn: () =>
      apiRequest<ReviewIssue[]>(
        `/v1/review-issues/version/${encodeURIComponent(versionId ?? "")}`,
        { accessToken: accessToken ?? undefined },
      ),
    enabled: Boolean(accessToken && versionId),
    refetchInterval: refetchPendingInterpretation,
  });

  return { records, issues, pages };
}

export function useCreateFactRecord(versionId: string) {
  return useBaseReviewMutation<
    { baseVersionId: string; category: string; displayOrder: number },
    FactRecord
  >(versionId, () => "/v1/fact-records", "POST");
}

export function useCreateFact(versionId: string) {
  return useBaseReviewMutation<
    {
      recordId: string;
      fieldKey: string;
      extractedValue?: null;
      reviewedValue: string;
      origin: string;
      reviewState: "pendente";
    },
    BaseFact
  >(versionId, () => "/v1/facts", "POST");
}

export function useAnswerReviewIssue(versionId: string) {
  return useBaseReviewMutation<
    {
      id: string;
      answer: {
        reviewState: "confirmado" | "corrigido" | "omitido";
        reviewedValue?: string;
      };
    },
    ReviewIssue
  >(
    versionId,
    ({ id }) => `/v1/review-issues/${encodeURIComponent(id)}/answer`,
    "PATCH",
    ({ answer }) => answer,
  );
}

export function useUpdateFactReviewState(versionId: string) {
  return useBaseReviewMutation<
    {
      id: string;
      reviewState: "corrigido" | "omitido";
      reviewedValue?: string;
    },
    BaseFact
  >(
    versionId,
    ({ id }) => `/v1/facts/${encodeURIComponent(id)}/review-state`,
    "PATCH",
    ({ reviewState, reviewedValue }) => ({ reviewState, reviewedValue }),
  );
}

export function useApproveGroundTruth(versionId: string) {
  const accessToken = useAuthStore((state) => state.accessToken);
  const queryClient = useQueryClient();

  return useMutation<GroundTruthSnapshot, Error, void>({
    mutationFn: () =>
      apiRequest<GroundTruthSnapshot>(
        `/v1/gt-snapshots/approve-version/${encodeURIComponent(versionId)}`,
        { method: "POST", accessToken: accessToken ?? undefined },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.records(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.issues(versionId),
      });
    },
  });
}

export function useCreateFactEvidence(versionId: string) {
  return useBaseReviewMutation<
    {
      factId: string;
      extractedPageId: string;
      sourceExcerpt: string;
      sourceLocator: { pageNumber: number | null };
    },
    FactEvidence
  >(versionId, () => "/v1/fact-evidence", "POST");
}

export function useExtractedPageText(pageId?: string) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery({
    queryKey: ["extracted-page-text", pageId ?? ""],
    queryFn: () =>
      extractedPageReadText(pageId ?? "", {
        accessToken: accessToken ?? undefined,
      }),
    enabled: Boolean(accessToken && pageId),
  });
}
