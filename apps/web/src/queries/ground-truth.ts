import {
  baseDocumentFindById,
  baseVersionFindById,
  gtSnapshotFindById,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { skipToken, useQuery } from "@tanstack/react-query";
import {
  groupSnapshotItems,
  type GroundTruthFacts,
} from "@/utils/ground-truth-facts";
import { useCurrentAccount } from "./current-account";

/**
 * Fatos do Ground Truth ativo da conta: snapshot aprovado, a versão e o documento que o
 * originaram. É a única fonte de "dados profissionais" do perfil — nunca um cadastro
 * paralelo (ver AGENTS.md, Currículo Base como Ground Truth).
 */
export function useActiveGroundTruth() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const account = useCurrentAccount();
  const snapshotId = account.data?.owner.activeSnapshotId ?? null;

  const snapshot = useQuery({
    queryKey: apiQueryKeys.groundTruth.snapshot(snapshotId ?? ""),
    queryFn:
      accessToken && snapshotId
        ? () => gtSnapshotFindById(snapshotId, { accessToken })
        : skipToken,
    staleTime: 60_000,
  });

  const baseVersionId = snapshot.data?.baseVersionId ?? null;
  const version = useQuery({
    queryKey: apiQueryKeys.groundTruth.version(baseVersionId ?? ""),
    queryFn:
      accessToken && baseVersionId
        ? () => baseVersionFindById(baseVersionId, { accessToken })
        : skipToken,
    staleTime: 60_000,
  });

  const documentId = version.data?.documentId ?? null;
  const document = useQuery({
    queryKey: apiQueryKeys.groundTruth.document(documentId ?? ""),
    queryFn:
      accessToken && documentId
        ? () => baseDocumentFindById(documentId, { accessToken })
        : skipToken,
    staleTime: 60_000,
  });

  const facts: GroundTruthFacts | null = snapshot.data
    ? groupSnapshotItems(snapshot.data.items)
    : null;

  return {
    isLoading:
      account.isLoading || (Boolean(snapshotId) && snapshot.isLoading),
    /** false enquanto a conta não tiver nenhum Ground Truth ativo. */
    hasActiveSnapshot: Boolean(snapshotId),
    snapshot: snapshot.data ?? null,
    version: version.data ?? null,
    document: document.data ?? null,
    facts,
  };
}
