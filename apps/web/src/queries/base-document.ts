import type { BaseDocumentUploadResponse } from "@lib/api/generated/model";
import {
  baseDocumentUpload,
  extractionRunFindByVersion,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
    onSuccess: (result, file) => {
      queryClient.setQueryData(
        apiQueryKeys.baseDocuments.detail(result.document.id),
        result.document,
      );
      if (useAuthStore.getState().accessToken === accessToken) {
        useBaseCvStore.getState().setUploadedDocument(file);
      }

      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseDocuments.byOwner(result.document.ownerId),
      });
    },
  });
}

export function useSelectBaseDocumentFile() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const selectFile = useBaseCvStore((state) => state.selectFile);
  const setError = useBaseCvStore((state) => state.setError);
  const upload = useUploadBaseDocument();

  return {
    upload,
    selectFile: (file: File) => {
      if (!selectFile(file)) return false;
      if (!accessToken) return true;

      upload.mutate(file, {
        onError: (error) => {
          if (useBaseCvStore.getState().file !== file) return;
          setError(
            error instanceof TypeError
              ? "Não foi possível conectar à API para enviar o arquivo."
              : error.message,
          );
        },
      });
      return true;
    },
  };
}

export function useBaseDocumentExtraction(versionId?: string) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery({
    queryKey: apiQueryKeys.extractionRuns.byVersion(versionId ?? ""),
    queryFn: () => {
      if (!accessToken || !versionId) {
        throw new Error("Uma sessão autenticada e versão são necessárias.");
      }
      return extractionRunFindByVersion(versionId, { accessToken });
    },
    enabled: Boolean(accessToken && versionId),
    refetchInterval: (query) => {
      const run = query.state.data?.[0];
      return run && ["QUEUED", "PROCESSING"].includes(run.status)
        ? 1500
        : false;
    },
  });
}
