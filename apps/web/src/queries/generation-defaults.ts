import { apiRequest } from "@lib/api/client";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

/**
 * Padrões de geração da conta (layout de currículo escolhido). Só contas guardam padrões. Chamadas
 * escritas à mão porque o cliente gerado ainda não inclui estas rotas.
 */
export interface GenerationDefaults {
  templateName: string | null;
}

const defaultsPath = (ownerId: string) =>
  `/v1/owners/${encodeURIComponent(ownerId)}/generation-defaults`;

export function useGenerationDefaults(
  ownerId: string | null,
  token: string | null,
) {
  return useQuery({
    queryKey: apiQueryKeys.generationDefaults.byOwner(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () =>
            apiRequest<GenerationDefaults>(defaultsPath(ownerId), {
              accessToken: token,
            })
        : skipToken,
  });
}

export function useSaveGenerationDefaults(
  ownerId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<GenerationDefaults, Error, string>({
    mutationFn: (templateName) => {
      if (!ownerId || !token)
        throw new Error("Entre na conta para salvar o padrão.");
      return apiRequest<GenerationDefaults>(defaultsPath(ownerId), {
        method: "PUT",
        accessToken: token,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ templateName }),
      });
    },
    onSuccess: (defaults) =>
      queryClient.setQueryData(
        apiQueryKeys.generationDefaults.byOwner(ownerId ?? ""),
        defaults,
      ),
  });
}
