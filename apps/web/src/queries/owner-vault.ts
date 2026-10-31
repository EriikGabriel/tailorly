import { apiRequest } from "@lib/api/client";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

/**
 * Preferências de segundo cérebro da conta: pasta do vault Obsidian (relativa à raiz do servidor),
 * leitura das notas, exportação dos currículos e busca web. Chamadas escritas à mão porque o cliente
 * gerado ainda não inclui estas rotas.
 */
export interface OwnerVaultSettings {
  rootAvailable: boolean;
  webSearchAvailable: boolean;
  relativePath: string | null;
  usable: boolean;
  readEnabled: boolean;
  exportEnabled: boolean;
  exportSubfolder: string | null;
  webSearchEnabled: boolean;
}

export interface OwnerVaultInput {
  relativePath: string;
  readEnabled: boolean;
  exportEnabled: boolean;
  exportSubfolder: string;
  webSearchEnabled: boolean;
}

const vaultPath = (ownerId: string) =>
  `/v1/owners/${encodeURIComponent(ownerId)}/vault`;

export function useOwnerVault(ownerId: string | null, token: string | null) {
  return useQuery({
    queryKey: apiQueryKeys.ownerVault.byOwner(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () =>
            apiRequest<OwnerVaultSettings>(vaultPath(ownerId), {
              accessToken: token,
            })
        : skipToken,
  });
}

export function useSaveOwnerVault(
  ownerId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<OwnerVaultSettings, Error, OwnerVaultInput>({
    mutationFn: (input) => {
      if (!ownerId || !token)
        throw new Error("Entre na conta para configurar.");
      return apiRequest<OwnerVaultSettings>(vaultPath(ownerId), {
        method: "PUT",
        accessToken: token,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
    },
    onSuccess: (settings) =>
      queryClient.setQueryData(
        apiQueryKeys.ownerVault.byOwner(ownerId ?? ""),
        settings,
      ),
  });
}

export function useClearOwnerVault(
  ownerId: string | null,
  token: string | null,
) {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    mutationFn: () => {
      if (!ownerId || !token)
        throw new Error("Entre na conta para configurar.");
      return apiRequest<void>(vaultPath(ownerId), {
        method: "DELETE",
        accessToken: token,
      });
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.ownerVault.byOwner(ownerId ?? ""),
      }),
  });
}
