import { apiRequest } from "@lib/api/client";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useCurrentAccount } from "@queries/current-account";
import { useViewer } from "@queries/cv-generation";
import {
  queryOptions,
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
  /** Item do currículo a que se refere (ex.: projects:2); nulo para links soltos e evidências antigas. */
  subjectKey: string | null;
  subjectLabel: string | null;
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

export function supplementaryEvidenceQueryOptions(
  snapshotId: string | null,
  token: string | null,
) {
  return queryOptions({
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

export function useSupplementaryEvidence(
  snapshotId: string | null,
  token: string | null,
) {
  return useQuery(supplementaryEvidenceQueryOptions(snapshotId, token));
}

/**
 * Snapshot ativo da conta, que é a quem pertencem as evidências. Visitantes anônimos não têm: o
 * snapshot ativo só é conhecido pela conta.
 */
export function useEvidenceSnapshot() {
  const viewer = useViewer();
  const account = useCurrentAccount();
  const snapshotId =
    viewer.kind === "account"
      ? (account.data?.owner.activeSnapshotId ?? null)
      : null;
  return { snapshotId, token: viewer.token };
}

/** Chave comum às coletas manual e automática, para a tela saber que alguma está rodando. */
export const EVIDENCE_COLLECT_KEY = [
  "supplementary-evidence",
  "collect",
] as const;

/**
 * Coleta as evidências dos links logo depois de aprovar o Currículo Base, em segundo plano, para
 * estarem prontas para a pessoa confirmar no gerador. Nada vai ao currículo sem essa confirmação.
 * Falhas são silenciosas: a coleta manual continua disponível no gerador.
 */
export function useAutoCollectSupplementaryEvidence() {
  const queryClient = useQueryClient();

  return useMutation<
    EvidenceCollection,
    Error,
    { snapshotId: string; token: string }
  >({
    mutationKey: EVIDENCE_COLLECT_KEY,
    meta: {
      notify: {
        // Falhas ficam silenciosas de propósito: a coleta manual segue no gerador.
        success: (collection: EvidenceCollection) =>
          collection.evidenceCreated > 0
            ? {
                kind: "success",
                title: "Evidências complementares coletadas",
                description: `${collection.evidenceCreated} para você confirmar no gerador.`,
                href: "/generator",
              }
            : null,
      },
    },
    mutationFn: ({ snapshotId, token }) =>
      apiRequest<EvidenceCollection>(
        `/v1/cv/evidence/snapshots/${encodeURIComponent(snapshotId)}/collect`,
        { method: "POST", accessToken: token },
      ),
    onSettled: (_data, _error, { snapshotId }) =>
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.supplementaryEvidence.snapshot(snapshotId),
      }),
  });
}

export function useCollectSupplementaryEvidence(
  snapshotId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<EvidenceCollection, Error, void>({
    mutationKey: EVIDENCE_COLLECT_KEY,
    meta: {
      notify: {
        success: (collection: EvidenceCollection) => ({
          kind: collection.evidenceCreated > 0 ? "success" : "info",
          title: "Coleta de evidências concluída",
          description:
            collection.evidenceCreated > 0
              ? `${collection.evidenceCreated} para você confirmar.`
              : "Nenhuma evidência nova encontrada nos links.",
          href: "/generator",
        }),
        error: (error) => ({
          kind: "error",
          title: "Coleta de evidências não concluída",
          description: error.message,
        }),
      },
    },
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
