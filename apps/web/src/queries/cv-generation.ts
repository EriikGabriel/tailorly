import { ApiClientError, apiBlob } from "@lib/api/client";
import { SESSION_EXPIRED_MESSAGE } from "@lib/api/error-message";
import type {
  CvGenerationResponse,
  CvTemplateResponse,
} from "@lib/api/generated/model";
import {
  cvGenerationDelete,
  cvGenerationFindById,
  cvGenerationFindByOwner,
  cvGenerationGenerate,
  cvTemplateBundled,
  cvTemplateCurrent,
  cvTemplateSave,
  getCvGenerationDownloadUrl,
  getCvGenerationPreviewUrl,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  isAnonymousSessionActive,
  useAnonymousSessionStore,
} from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";
import type { OwnerKind } from "@stores/base-cv-store";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { useGenerationPreferencesStore } from "@stores/generation-preferences-store";
import { useJobPostingStore } from "@stores/job-posting-store";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { ensureCredential, tokenForOwnerKind } from "./api-credential";
import { useCurrentOwnerId } from "./current-account";

/** Dono e token dos dados visíveis agora: a conta, se houver login; senão a sessão anônima. */
export function useViewer(): {
  ownerId: string | null;
  token: string | null;
  kind: OwnerKind | null;
} {
  const accountToken = useAuthStore((state) => state.accessToken);
  const session = useAnonymousSessionStore((state) =>
    isAnonymousSessionActive(state.session) ? state.session : null,
  );
  const accountOwnerId = useCurrentOwnerId().data ?? null;
  if (accountToken)
    return { ownerId: accountOwnerId, token: accountToken, kind: "account" };
  if (session)
    return {
      ownerId: session.ownerId,
      token: session.token,
      kind: "anonymous",
    };
  return { ownerId: null, token: null, kind: null };
}

/**
 * Gera o currículo da vaga selecionada com as preferências ativas. A esteira (IA + compilação +
 * avaliação de ATS) roda no servidor e pode levar alguns segundos.
 */
export function useGenerateCv() {
  const queryClient = useQueryClient();

  return useMutation<CvGenerationResponse, Error, void>({
    mutationFn: async () => {
      const selected = useJobPostingStore.getState().selected;
      if (!selected)
        throw new Error("Leia uma vaga antes de gerar o currículo.");
      const credential = await ensureCredential(queryClient);
      if (credential.kind !== selected.ownerKind) {
        throw new Error("A vaga pertence a outra sessão. Leia a vaga de novo.");
      }
      const { guidelines, optimizeForAts } =
        useGenerationPreferencesStore.getState();
      return cvGenerationGenerate(
        {
          ownerId: credential.ownerId,
          jobPostingId: selected.postingId,
          ...(selected.versionNumber
            ? { jobVersionNumber: selected.versionNumber }
            : {}),
          preferences: { guidelines, optimizeForAts },
        },
        { accessToken: credential.token },
      );
    },
    onSuccess: (generation) => {
      const kind = useJobPostingStore.getState().selected?.ownerKind;
      if (generation.id && kind) {
        useCvGenerationStore.getState().select(generation.id, kind);
        queryClient.setQueryData(
          apiQueryKeys.cv.generation(generation.id),
          generation,
        );
      }
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.cv.all });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
      });
    },
  });
}

/** A geração aberta no gerador, com a nota de ATS e as iterações da esteira. */
export function useSelectedGeneration() {
  const selected = useCvGenerationStore((state) => state.selected);
  const token = tokenForOwnerKind(selected?.ownerKind ?? null);
  const id = selected?.id;

  return useQuery<CvGenerationResponse>({
    queryKey: apiQueryKeys.cv.generation(id ?? ""),
    queryFn:
      id && token
        ? async () => {
            try {
              return await cvGenerationFindById(id, { accessToken: token });
            } catch (error) {
              if (
                error instanceof ApiClientError &&
                [401, 403, 404].includes(error.status)
              ) {
                useCvGenerationStore.getState().clear();
              }
              throw error;
            }
          }
        : skipToken,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  });
}

/** Histórico real do owner atual; vazio para quem ainda não gerou nada. */
export function useCvHistory() {
  const { ownerId, token } = useViewer();

  return useQuery<CvGenerationResponse[]>({
    queryKey: apiQueryKeys.cv.generations(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () => cvGenerationFindByOwner(ownerId, { accessToken: token })
        : skipToken,
    retry: false,
  });
}

export function useDeleteCvGeneration() {
  const queryClient = useQueryClient();
  const viewer = useViewer();

  return useMutation<void, Error, string>({
    mutationFn: (id) => {
      if (!viewer.token) throw new Error(SESSION_EXPIRED_MESSAGE);
      return cvGenerationDelete(id, { accessToken: viewer.token });
    },
    onSuccess: (_, id) => {
      if (useCvGenerationStore.getState().selected?.id === id) {
        useCvGenerationStore.getState().clear();
      }
      queryClient.invalidateQueries({ queryKey: apiQueryKeys.cv.all });
    },
  });
}

/** Template Typst que as próximas gerações usam (a última versão editada ou o embarcado). */
export function useCurrentCvTemplate(
  ownerId: string | null,
  token: string | null,
) {
  return useQuery<CvTemplateResponse>({
    queryKey: apiQueryKeys.cv.template(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () => cvTemplateCurrent(ownerId, { accessToken: token })
        : skipToken,
    retry: false,
  });
}

export function useBundledCvTemplate(token: string | null) {
  return useQuery<CvTemplateResponse>({
    queryKey: apiQueryKeys.cv.bundledTemplate,
    queryFn: token
      ? () => cvTemplateBundled({ accessToken: token })
      : skipToken,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  });
}

export function useSaveCvTemplate() {
  const queryClient = useQueryClient();

  return useMutation<
    CvTemplateResponse,
    Error,
    { ownerId: string; token: string; content: string }
  >({
    mutationFn: ({ ownerId, token, content }) =>
      cvTemplateSave({ ownerId, content }, { accessToken: token }),
    onSuccess: (_, { ownerId }) => {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.cv.template(ownerId),
      });
    },
  });
}

/** Compila o código Typst com o conteúdo da geração; não grava nada. */
export function previewCvPdf(
  generationId: string,
  content: string,
  token: string,
  signal?: AbortSignal,
): Promise<Blob> {
  return apiBlob(getCvGenerationPreviewUrl(generationId), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content }),
    accessToken: token,
    accept: "application/pdf",
    signal,
  });
}

/** Baixa o arquivo gerado e salva pelo navegador. */
export async function downloadCvFile(
  generationId: string,
  format: "pdf" | "markdown",
  token: string,
): Promise<void> {
  const blob = await apiBlob(
    getCvGenerationDownloadUrl(generationId, { format }),
    { accessToken: token },
  );
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = format === "pdf" ? "curriculo.pdf" : "curriculo.md";
  link.click();
  URL.revokeObjectURL(url);
}
