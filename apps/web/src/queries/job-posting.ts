import type { AnalyzeResult, JobPostingInput } from "@@types/job-posting";
import { ApiClientError } from "@lib/api/client";
import type {
  JobPostingDetailResponse,
  JobPostingVersionResponse,
} from "@lib/api/generated/model";
import {
  jobPostingCreate,
  jobPostingDelete,
  jobPostingFindById,
  jobPostingFindByOwner,
  jobPostingFindVersion,
  jobPostingReanalyze,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  isAnonymousSessionActive,
  useAnonymousSessionStore,
} from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";
import { useJobPostingStore } from "@stores/job-posting-store";
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useEffect } from "react";
import { jobPostingInputError } from "@/utils/job-posting-input";
import {
  type ApiCredential,
  ensureCredential,
  tokenForOwnerKind,
} from "./api-credential";
import { useCurrentOwnerId } from "./current-account";

/** A vaga selecionada deixou de ser acessível (migração, login, sessão expirada ou exclusão). */
function dropSelectionWhenInaccessible(postingId: string, error: unknown) {
  if (
    error instanceof ApiClientError &&
    [401, 403, 404].includes(error.status) &&
    useJobPostingStore.getState().selected?.postingId === postingId
  ) {
    useJobPostingStore.getState().clear();
  }
}

/** Lê o link ou o texto, grava a vaga como nova versão (ou reaproveita) e a seleciona. */
export function useAnalyzeJobPosting() {
  const queryClient = useQueryClient();

  return useMutation<AnalyzeResult, Error, JobPostingInput>({
    mutationKey: [...apiQueryKeys.jobPostings.all, "create"],
    mutationFn: async (input) => {
      // Antes de qualquer chamada (inclusive a que emite a sessão anônima): o que o servidor
      // recusaria não deve gastar requisição, cota nem uma sessão nova.
      const invalid = jobPostingInputError(input);
      if (invalid) throw new Error(invalid);

      const send = async (
        credential: ApiCredential,
      ): Promise<AnalyzeResult> => ({
        ...(await jobPostingCreate(
          {
            ownerId: credential.ownerId,
            ...(input.mode === "url"
              ? { url: input.url.trim() }
              : { text: input.text }),
          },
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
    onSuccess: (result) => {
      if (result.jobPostingId) {
        useJobPostingStore
          .getState()
          .select(result.jobPostingId, result.ownerKind);
        queryClient.invalidateQueries({
          queryKey: apiQueryKeys.jobPostings.detail(result.jobPostingId),
        });
      }
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.jobPostings.all,
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
      });
    },
  });
}

/** Vaga selecionada com a versão mais recente e o histórico de versões. */
export function useSelectedJobPosting() {
  const selected = useJobPostingStore((state) => state.selected);
  const accountToken = useAuthStore((state) => state.accessToken);
  const anonymousToken = useAnonymousSessionStore((state) =>
    isAnonymousSessionActive(state.session) ? state.session.token : null,
  );
  const token =
    selected?.ownerKind === "account"
      ? accountToken
      : selected?.ownerKind === "anonymous"
        ? anonymousToken
        : null;
  const postingId = selected?.postingId;

  // Sem a credencial do dono (logout, sessão expirada ou descartada) a vaga não abre mais.
  useEffect(() => {
    if (selected && !token) useJobPostingStore.getState().clear();
  }, [selected, token]);

  return useQuery<JobPostingDetailResponse>({
    queryKey: apiQueryKeys.jobPostings.detail(postingId ?? ""),
    queryFn:
      postingId && token
        ? async () => {
            try {
              return await jobPostingFindById(postingId, {
                accessToken: token,
              });
            } catch (error) {
              dropSelectionWhenInaccessible(postingId, error);
              throw error;
            }
          }
        : skipToken,
    retry: false,
  });
}

/** Versão exibida: a escolhida no histórico ou a mais recente do detalhe. */
export function useJobPostingVersion(
  detail: JobPostingDetailResponse | undefined,
) {
  const selected = useJobPostingStore((state) => state.selected);
  const token = selected ? tokenForOwnerKind(selected.ownerKind) : null;
  const postingId = selected?.postingId;
  const wanted = selected?.versionNumber ?? null;
  const latest = detail?.latest;
  const isOlder =
    wanted !== null && latest !== undefined && wanted !== latest.versionNumber;

  const older = useQuery<JobPostingVersionResponse>({
    queryKey: apiQueryKeys.jobPostings.version(postingId ?? "", wanted ?? 0),
    queryFn:
      isOlder && postingId && token && wanted !== null
        ? () => jobPostingFindVersion(postingId, wanted, { accessToken: token })
        : skipToken,
    retry: false,
  });

  return { version: isOlder ? older.data : latest, isLoading: older.isLoading };
}

export function useReanalyzeJobPosting() {
  const queryClient = useQueryClient();

  return useMutation<
    JobPostingVersionResponse,
    Error,
    { postingId: string; versionNumber: number }
  >({
    mutationFn: ({ postingId, versionNumber }) => {
      const selected = useJobPostingStore.getState().selected;
      const token = tokenForOwnerKind(selected?.ownerKind ?? null);
      if (!token) throw new Error("Sessão indisponível. Envie a vaga de novo.");
      return jobPostingReanalyze(postingId, versionNumber, {
        accessToken: token,
      });
    },
    onSuccess: (_, { postingId }) => {
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.jobPostings.all,
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.jobPostings.detail(postingId),
      });
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.anonymousSession.current,
      });
    },
  });
}

/** Exclui a vaga e todas as suas versões no servidor. */
export function useDeleteJobPosting() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: (postingId) => {
      const selected = useJobPostingStore.getState().selected;
      const token = tokenForOwnerKind(selected?.ownerKind ?? null);
      if (!token) throw new Error("Sessão indisponível.");
      return jobPostingDelete(postingId, { accessToken: token });
    },
    onSuccess: (_, postingId) => {
      queryClient.removeQueries({
        queryKey: apiQueryKeys.jobPostings.detail(postingId),
      });
      useJobPostingStore.getState().clear();
      queryClient.invalidateQueries({
        queryKey: apiQueryKeys.jobPostings.all,
      });
    },
  });
}

/** Vagas salvas do owner atual (conta ou sessão anônima já emitida). */
export function useJobPostingList() {
  const accountOwnerId = useCurrentOwnerId().data;
  const anonymousSession = useAnonymousSessionStore((state) =>
    isAnonymousSessionActive(state.session) ? state.session : null,
  );
  const accountToken = useAuthStore((state) => state.accessToken);

  const ownerId = accountToken ? accountOwnerId : anonymousSession?.ownerId;
  const token = accountToken ?? anonymousSession?.token ?? null;

  return useQuery({
    queryKey: apiQueryKeys.jobPostings.byOwner(ownerId ?? ""),
    queryFn:
      ownerId && token
        ? () => jobPostingFindByOwner(ownerId, { accessToken: token })
        : skipToken,
    retry: false,
  });
}
