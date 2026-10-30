import { SESSION_EXPIRED_MESSAGE } from "@lib/api/error-message";
import type { OwnerResponse } from "@lib/api/generated/model";
import {
  baseVersionDelete,
  ownerDeactivateSnapshot,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useViewer } from "./cv-generation";

/**
 * Desativa o Ground Truth ativo do owner atual: a conta ou sessão fica sem Ground Truth ativo,
 * mas o currículo base, os fatos e o histórico de gerações continuam intactos para auditoria.
 */
export function useDeactivateGroundTruth() {
  const queryClient = useQueryClient();
  const viewer = useViewer();

  return useMutation<OwnerResponse, Error, void>({
    mutationFn: () => {
      if (!viewer.token || !viewer.ownerId)
        throw new Error(SESSION_EXPIRED_MESSAGE);
      return ownerDeactivateSnapshot(viewer.ownerId, {
        accessToken: viewer.token,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.account.current });
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.cv.all });
    },
  });
}

/**
 * Exclui a versão do Currículo Base: arquivo, texto extraído, fatos, pendências, o snapshot do
 * Ground Truth aprovado a partir dela e os currículos gerados derivados desaparecem junto.
 */
export function useDeleteBaseVersion() {
  const queryClient = useQueryClient();
  const viewer = useViewer();

  return useMutation<void, Error, string>({
    mutationFn: (versionId) => {
      if (!viewer.token) throw new Error(SESSION_EXPIRED_MESSAGE);
      return baseVersionDelete(versionId, { accessToken: viewer.token });
    },
    onSuccess: () => {
      useBaseCvStore.getState().reset();
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.account.current });
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.cv.all });
      if (viewer.ownerId) {
        queryClient.invalidateQueries({
          queryKey: apiQueryKeys.baseDocuments.byOwner(viewer.ownerId),
        });
      }
    },
  });
}
