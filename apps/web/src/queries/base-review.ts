import type {
  ApiReviewIssue,
  BaseFact,
  FactEvidence,
  FactRecord,
  GroundTruthSnapshot,
} from "@@types/base-review";
import { apiRequest } from "@lib/api/client";
import { SESSION_EXPIRED_MESSAGE } from "@lib/api/error-message";
import {
  extractedPageFindAllByRunId,
  extractedPageReadText,
  extractionRunFindByVersion,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import {
  extractionJustCompleted,
  extractionRunInterval,
  interpretationRefetchInterval,
} from "@/utils/extraction-polling";
import { useApiToken } from "./api-credential";

// Centraliza as mutações da revisão para invalidar os dados exibidos na tela.
function useBaseReviewMutation<TInput, TOutput>(
  versionId: string,
  path: (input: TInput) => string,
  method: "POST" | "PATCH",
  requestBody: (input: TInput) => unknown = (input) => input,
  // Quem envia várias respostas seguidas invalida uma única vez ao final, para não
  // refazer as consultas a cada resposta (e não estourar o limite de taxa da sessão).
  invalidateOnSuccess = true,
) {
  const accessToken = useApiToken();
  const queryClient = useQueryClient();

  return useMutation<TOutput, Error, TInput>({
    mutationFn: (input) => {
      // Sem credencial a chamada só voltaria 401; avisa antes, com a orientação certa.
      if (!accessToken) throw new Error(SESSION_EXPIRED_MESSAGE);
      return apiRequest<TOutput>(path(input), {
        method,
        accessToken,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody(input)),
      });
    },
    onSuccess: () => {
      if (!invalidateOnSuccess) return;
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.records(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.issues(versionId),
      });
    },
  });
}

export function useInvalidateBaseReview(versionId: string) {
  const queryClient = useQueryClient();

  return () => {
    queryClient.invalidateQueries({
      queryKey: apiQueryKeys.baseReview.records(versionId),
    });
    queryClient.invalidateQueries({
      queryKey: apiQueryKeys.baseReview.issues(versionId),
    });
  };
}

export function useBaseReview(versionId?: string) {
  const accessToken = useApiToken();
  const queryClient = useQueryClient();
  const runs = useQuery({
    queryKey: apiQueryKeys.extractionRuns.byVersion(versionId ?? ""),
    queryFn: () =>
      extractionRunFindByVersion(versionId ?? "", {
        accessToken: accessToken ?? undefined,
      }),
    enabled: Boolean(accessToken && versionId),
    refetchInterval: (query) => extractionRunInterval(query.state.data?.[0]),
  });
  const completedRun = runs.data?.find((run) => run.status === "COMPLETED");
  const latestRun = runs.data?.[0];

  // Os fatos só existem quando a execução conclui: em vez de consultá-los a cada ciclo enquanto ela
  // roda, busca uma vez na transição para "concluída".
  const previousStatus = useRef<string | undefined>(undefined);
  useEffect(() => {
    const status = latestRun?.status;
    if (versionId && extractionJustCompleted(previousStatus.current, status)) {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.records(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.issues(versionId),
      });
    }
    previousStatus.current = status;
  }, [latestRun?.status, versionId, queryClient]);

  const refetchPendingInterpretation = (query: {
    state: { data?: { length: number } };
  }) =>
    interpretationRefetchInterval({
      latestRun,
      loadedCount: query.state.data?.length,
    });

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
      apiRequest<ApiReviewIssue[]>(
        `/v1/review-issues/version/${encodeURIComponent(versionId ?? "")}`,
        { accessToken: accessToken ?? undefined },
      ),
    enabled: Boolean(accessToken && versionId),
    refetchInterval: refetchPendingInterpretation,
  });

  return { records, issues, pages };
}

/** Consultas da revisão (fatos, pendências e páginas extraídas) de uma versão do Currículo Base. */
export type BaseReviewQueries = ReturnType<typeof useBaseReview>;

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
    ApiReviewIssue
  >(
    versionId,
    ({ id }) => `/v1/review-issues/${encodeURIComponent(id)}/answer`,
    "PATCH",
    ({ answer }) => answer,
    false,
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
  const accessToken = useApiToken();
  const queryClient = useQueryClient();

  return useMutation<GroundTruthSnapshot, Error, void>({
    mutationFn: () => {
      // Sem credencial ou versão a chamada só voltaria 401/404; avisa antes, com a orientação certa.
      if (!accessToken) throw new Error(SESSION_EXPIRED_MESSAGE);
      if (!versionId) {
        throw new Error("Envie o Currículo Base antes de aprová-lo.");
      }
      return apiRequest<GroundTruthSnapshot>(
        `/v1/gt-snapshots/approve-version/${encodeURIComponent(versionId)}`,
        { method: "POST", accessToken },
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.records(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseReview.issues(versionId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
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
  const accessToken = useApiToken();

  return useQuery({
    queryKey: ["extracted-page-text", pageId ?? ""],
    queryFn: () =>
      extractedPageReadText(pageId ?? "", {
        accessToken: accessToken ?? undefined,
      }),
    enabled: Boolean(accessToken && pageId),
  });
}
