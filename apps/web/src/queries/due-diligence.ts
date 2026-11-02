import {
  type CreateDueDiligenceInput,
  type DueDiligenceResponse,
  type DueDiligenceSummary,
  dueDiligenceCreate,
  dueDiligenceDelete,
  dueDiligenceFindById,
  dueDiligenceFindByOwner,
} from "@lib/api/due-diligence";
import {
  apiErrorMessage,
  SESSION_EXPIRED_MESSAGE,
} from "@lib/api/error-message";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useViewer } from "./cv-generation";

/** Análises salvas do owner atual (conta ou sessão anônima), da mais recente para a mais antiga. */
export function useDueDiligenceHistory() {
  const { ownerId, token } = useViewer();

  return useQuery<DueDiligenceSummary[]>({
    queryKey: apiQueryKeys.dueDiligence.byOwner(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () => dueDiligenceFindByOwner(ownerId, { accessToken: token })
        : skipToken,
    retry: false,
    // A esteira cria análises em segundo plano: confere até elas terminarem.
    refetchInterval: (query) =>
      query.state.data?.some((item) => item.status === "running")
        ? 4000
        : false,
  });
}

/** Relatório completo; é imutável depois de gravado, então não precisa ser revalidado. */
export function useDueDiligence(id: string | null) {
  const { token } = useViewer();

  return useQuery<DueDiligenceResponse>({
    queryKey: apiQueryKeys.dueDiligence.detail(id ?? ""),
    queryFn:
      id && token
        ? () => dueDiligenceFindById(id, { accessToken: token })
        : skipToken,
    retry: false,
    // Um relatório concluído é imutável; enquanto roda, confere até terminar.
    staleTime: (query) =>
      query.state.data?.status === "running" ? 0 : Number.POSITIVE_INFINITY,
    refetchInterval: (query) =>
      query.state.data?.status === "running" ? 4000 : false,
  });
}

export function useCreateDueDiligence() {
  const queryClient = useQueryClient();
  const { ownerId, token } = useViewer();

  return useMutation<
    DueDiligenceResponse,
    Error,
    Omit<CreateDueDiligenceInput, "ownerId">
  >({
    meta: {
      notify: {
        success: (created: DueDiligenceResponse) => ({
          kind: "success",
          title: "Due diligence concluída",
          description: `${created.companyName} · ${
            created.report?.factors.filter(
              (factor) => factor.status === "evidence_found",
            ).length ?? 0
          }/11 fatores com fontes`,
          href: "/due-diligence",
        }),
        error: (error) => ({
          kind: "error",
          title: "Due diligence não concluída",
          description: apiErrorMessage(
            error,
            "Não foi possível consultar as fontes públicas.",
          ),
          href: "/due-diligence",
        }),
      },
    },
    mutationFn: (input) => {
      if (!ownerId || !token) throw new Error(SESSION_EXPIRED_MESSAGE);
      return dueDiligenceCreate({ ownerId, ...input }, { accessToken: token });
    },
    onSuccess: (created) => {
      queryClient.setQueryData(
        apiQueryKeys.dueDiligence.detail(created.id),
        created,
      );
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.dueDiligence.all,
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
      });
    },
  });
}

export function useDeleteDueDiligence() {
  const queryClient = useQueryClient();
  const { token } = useViewer();

  return useMutation<void, Error, string>({
    mutationFn: (id) => {
      if (!token) throw new Error(SESSION_EXPIRED_MESSAGE);
      return dueDiligenceDelete(id, { accessToken: token });
    },
    onSuccess: (_, id) => {
      queryClient.removeQueries({
        queryKey: apiQueryKeys.dueDiligence.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.dueDiligence.all,
      });
    },
  });
}
