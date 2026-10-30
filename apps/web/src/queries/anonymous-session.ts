import type { AnonymousMigrationResponse } from "@lib/api/generated/model";
import {
  anonymousSessionCurrent,
  anonymousSessionEnd,
  anonymousSessionMigrate,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import {
  isAnonymousSessionActive,
  useAnonymousSessionStore,
} from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobPostingStore } from "@stores/job-posting-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

/** Uso, limites e se há snapshot revisado migrável da sessão anônima atual. */
export function useAnonymousSessionStatus() {
  const token = useAnonymousSessionStore((state) =>
    isAnonymousSessionActive(state.session) ? state.session.token : undefined,
  );

  return useQuery({
    queryKey: [...apiQueryKeys.anonymousSession.current, token ?? ""],
    queryFn: () => anonymousSessionCurrent({ accessToken: token }),
    enabled: Boolean(token),
    staleTime: 10_000,
    retry: false,
  });
}

/** As vagas do visitante não migram para a conta: somem com a sessão. */
function releaseVisitorJobPosting() {
  if (useJobPostingStore.getState().selected?.ownerKind === "anonymous") {
    useJobPostingStore.getState().clear();
  }
}

function releaseVisitorData() {
  useAnonymousSessionStore.getState().clear();
  releaseVisitorJobPosting();
  // Uma versão que era do visitante some do cliente; se foi migrada, passa a ser da conta.
  if (useBaseCvStore.getState().ownerKind === "anonymous") {
    useBaseCvStore.getState().reset();
  }
}

export function useMigrateAnonymousSession() {
  const queryClient = useQueryClient();

  return useMutation<
    AnonymousMigrationResponse,
    Error,
    { replaceActive: boolean }
  >({
    mutationFn: ({ replaceActive }) => {
      const session = useAnonymousSessionStore.getState().session;
      const accountToken = useAuthStore.getState().accessToken;
      if (!accountToken) {
        throw new Error(
          "Entre na sua conta para trazer o currículo de visitante.",
        );
      }
      // Sessão vencida já foi apagada no servidor: migrar só levaria uma recusa.
      if (!isAnonymousSessionActive(session)) {
        throw new Error(
          "A sessão de visitante expirou e os dados dela foram removidos.",
        );
      }
      return anonymousSessionMigrate(
        { sessionToken: session.token, replaceActive },
        { accessToken: accountToken },
      );
    },
    onSuccess: () => {
      useAnonymousSessionStore.getState().clear();
      releaseVisitorJobPosting();
      // A versão migrada mantém o mesmo id; só passa a ser acessada pela conta.
      if (useBaseCvStore.getState().ownerKind === "anonymous") {
        useBaseCvStore.getState().setOwnerKind("account");
      }
      queryClient.invalidateQueries();
    },
  });
}

/** Encerra a sessão e apaga na hora os dados do visitante no servidor. */
export function useDiscardAnonymousSession() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    mutationFn: async () => {
      const session = useAnonymousSessionStore.getState().session;
      // Sem sessão ou já vencida (o servidor apagou os dados): basta limpar o cliente.
      if (!isAnonymousSessionActive(session)) return;
      await anonymousSessionEnd({ accessToken: session.token });
    },
    onSuccess: () => {
      releaseVisitorData();
      queryClient.invalidateQueries();
    },
  });
}
