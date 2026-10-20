import type { BaseDocumentUploadResponse } from "@lib/api/generated/model";
import { baseDocumentUpload } from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { currentAccountQueryOptions } from "./current-account";

export function useUploadBaseDocument() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const queryClient = useQueryClient();

  return useMutation<BaseDocumentUploadResponse, Error, File>({
    mutationKey: [...apiQueryKeys.baseDocuments.all, "upload"],
    mutationFn: async (file) => {
      if (!accessToken) {
        throw new Error("Entre na sua conta antes de enviar o currículo.");
      }

      const account = await queryClient.fetchQuery(
        currentAccountQueryOptions(accessToken),
      );
      return baseDocumentUpload(
        { ownerId: account.owner.id },
        { file },
        { accessToken },
      );
    },
    onSuccess: (result) => {
      queryClient.setQueryData(
        apiQueryKeys.baseDocuments.detail(result.document.id),
        result.document,
      );

      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseDocuments.byOwner(result.document.ownerId),
      });
    },
  });
}
