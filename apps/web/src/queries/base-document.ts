import { ApiClientError } from "@lib/api/client";
import type { BaseDocumentUploadResponse } from "@lib/api/generated/model";
import {
  baseDocumentUpload,
  extractionRunFindByVersion,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { type OwnerKind, useBaseCvStore } from "@stores/base-cv-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { extractionRunInterval } from "@/utils/extraction-polling";
import {
  type ApiCredential,
  ensureCredential,
  useApiToken,
} from "./api-credential";

export type UploadResult = BaseDocumentUploadResponse & {
  ownerKind: OwnerKind;
};

export function useUploadBaseDocument() {
  const queryClient = useQueryClient();

  return useMutation<UploadResult, Error, File>({
    mutationKey: [...apiQueryKeys.baseDocuments.all, "upload"],
    mutationFn: async (file) => {
      const send = async (
        credential: ApiCredential,
      ): Promise<UploadResult> => ({
        ...(await baseDocumentUpload(
          { ownerId: credential.ownerId },
          { file },
          { accessToken: credential.token },
        )),
        ownerKind: credential.kind,
      });

      const credential = await ensureCredential(queryClient);
      try {
        return await send(credential);
      } catch (error) {
        // Sessão anônima expirada/revogada: o cliente já a descartou; emite outra e repete uma vez.
        if (
          credential.kind === "anonymous" &&
          error instanceof ApiClientError &&
          error.status === 401
        ) {
          return send(await ensureCredential(queryClient));
        }
        throw error;
      }
    },
    onSuccess: (result, file) => {
      queryClient.setQueryData(
        apiQueryKeys.baseDocuments.detail(result.document.id),
        result.document,
      );
      useBaseCvStore
        .getState()
        .setUploadedDocument(file, result.version.id, result.ownerKind);

      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.baseDocuments.byOwner(result.document.ownerId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
      });
    },
  });
}

export function useSelectBaseDocumentFile() {
  const selectFile = useBaseCvStore((state) => state.selectFile);
  const setError = useBaseCvStore((state) => state.setError);
  const upload = useUploadBaseDocument();

  return {
    upload,
    selectFile: (file: File) => {
      const store = useBaseCvStore.getState();
      const sameFile = store.file === file;
      if (!selectFile(file)) return false;
      // O mesmo arquivo já enviado (ou em envio) não é enviado de novo. Um arquivo escolhido de novo
      // no seletor é outro objeto `File`, então o reenvio idêntico continua indo ao servidor, que
      // reavalia os fatos sensíveis ao tempo.
      const alreadySent =
        store.selectionId !== null &&
        store.uploadedSelectionId === store.selectionId;
      if (sameFile && (alreadySent || upload.isPending)) return true;

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
  const accessToken = useApiToken();

  return useQuery({
    queryKey: apiQueryKeys.extractionRuns.byVersion(versionId ?? ""),
    queryFn: () => {
      if (!accessToken || !versionId) {
        throw new Error("Uma sessão autenticada e versão são necessárias.");
      }
      return extractionRunFindByVersion(versionId, { accessToken });
    },
    enabled: Boolean(accessToken && versionId),
    refetchInterval: (query) => extractionRunInterval(query.state.data?.[0]),
  });
}
