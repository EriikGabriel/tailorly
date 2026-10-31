import { apiRequest } from "@lib/api/client";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

/**
 * Evidências complementares (ver README, "Enriquecimento da geração"). São detalhes encontrados
 * nos links do Currículo Base; só as confirmadas pela pessoa podem entrar no currículo.
 * Chamadas escritas à mão porque o cliente gerado ainda não inclui estas rotas.
 */
export type EvidenceReviewState = "pendente" | "confirmado" | "descartado";

export interface SupplementaryEvidence {
  id: string;
  sourceKind: string;
  sourceUrl: string | null;
  sourceExcerpt: string;
  claim: string;
  reviewState: EvidenceReviewState;
  collectedAt: string;
}

export interface EvidenceCollection {
  linksFound: number;
  linksAlreadyKnown: number;
  linksFailed: number;
  evidenceCreated: number;
  aiStatus: "completed" | "limit_reached" | "unavailable";
}

export function useSupplementaryEvidence(
  snapshotId: string | null,
  token: string | null,
) {
  return useQuery({
    queryKey: apiQueryKeys.supplementaryEvidence.snapshot(snapshotId ?? ""),
    queryFn:
      snapshotId && token
        ? () =>
            apiRequest<SupplementaryEvidence[]>(
              `/v1/cv/evidence/snapshots/${encodeURIComponent(snapshotId)}`,
              { accessToken: token },
            )
        : skipToken,
  });
}

export function useCollectSupplementaryEvidence(
  snapshotId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<EvidenceCollection, Error, void>({
    mutationFn: () => {
      if (!snapshotId || !token) throw new Error("Nenhum Ground Truth ativo.");
      return apiRequest<EvidenceCollection>(
        `/v1/cv/evidence/snapshots/${encodeURIComponent(snapshotId)}/collect`,
        { method: "POST", accessToken: token },
      );
    },
    onSettled: () =>
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.supplementaryEvidence.snapshot(snapshotId ?? ""),
      }),
  });
}

export function useReviewSupplementaryEvidence(
  snapshotId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<
    SupplementaryEvidence,
    Error,
    { id: string; reviewState: EvidenceReviewState }
  >({
    mutationFn: ({ id, reviewState }) => {
      if (!token) throw new Error("Sessão expirada. Entre de novo.");
      return apiRequest<SupplementaryEvidence>(
        `/v1/cv/evidence/${encodeURIComponent(id)}/review-state`,
        {
          method: "PATCH",
          accessToken: token,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ reviewState }),
        },
      );
    },
    onSettled: () =>
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.supplementaryEvidence.snapshot(snapshotId ?? ""),
      }),
  });
}
