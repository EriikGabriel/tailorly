import { apiRequest } from "@api/client";
import { apiQueryKeys } from "@api/query-keys";
import type { CvLanguagePreference } from "@constants/generation-options";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

/**
 * Padrões de geração da conta, gravados no perfil: layout, otimização para ATS (ligada até a conta
 * mudar), diretrizes de destaque e idioma do currículo. São o ponto de partida do gerador, que ajusta diretrizes e layout
 * por vaga sem gravar aqui. Só contas guardam padrões. Chamadas escritas à mão porque o cliente
 * gerado ainda não inclui estas rotas.
 */
export interface GenerationDefaults {
  templateName: string | null;
  optimizeForAts: boolean;
  guidelines: string[];
  /** Idioma do currículo final; `auto` detecta pela vaga. */
  language: CvLanguagePreference;
}

/** Atualização parcial: só os campos enviados mudam no servidor. */
export type GenerationDefaultsInput = Partial<GenerationDefaults>;

export const DEFAULT_TEMPLATE = "classic";

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

  return useMutation<GenerationDefaults, Error, GenerationDefaultsInput>({
    mutationFn: (input) => {
      if (!ownerId || !token)
        throw new Error("Entre na conta para salvar o padrão.");
      return apiRequest<GenerationDefaults>(defaultsPath(ownerId), {
        method: "PUT",
        accessToken: token,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
    },
    onSuccess: (defaults) =>
      queryClient.setQueryData(
        apiQueryKeys.generationDefaults.byOwner(ownerId ?? ""),
        defaults,
      ),
  });
}
