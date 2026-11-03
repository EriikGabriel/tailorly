import { apiRequest } from "@api/client";
import { apiQueryKeys } from "@api/query-keys";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

/**
 * Preferência da conta sobre a etapa automática de due diligence: quando ligada, a leitura de uma vaga
 * consulta a empresa em segundo plano (gasta busca web e IA). Desligada, só a consulta manual na página roda.
 */
export interface DueDiligencePreferences {
  pipelineEnabled: boolean;
}

const preferencesPath = (ownerId: string) =>
  `/v1/owners/${encodeURIComponent(ownerId)}/due-diligence-preferences`;

export function useDueDiligencePreferences(
  ownerId: string | null,
  token: string | null,
) {
  return useQuery({
    queryKey: apiQueryKeys.dueDiligence.preferences(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () =>
            apiRequest<DueDiligencePreferences>(preferencesPath(ownerId), {
              accessToken: token,
            })
        : skipToken,
  });
}

/** Salva o interruptor junto com as demais alterações do perfil (pela barra de salvar). */
export function useSaveDueDiligencePreferences(
  ownerId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<DueDiligencePreferences, Error, DueDiligencePreferences>({
    mutationFn: (input) => {
      if (!ownerId || !token)
        throw new Error("Entre na conta para salvar a preferência.");
      return apiRequest<DueDiligencePreferences>(preferencesPath(ownerId), {
        method: "PUT",
        accessToken: token,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
    },
    onSuccess: (preferences) =>
      queryClient.setQueryData(
        apiQueryKeys.dueDiligence.preferences(ownerId ?? ""),
        preferences,
      ),
  });
}
