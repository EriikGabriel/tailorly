import type {
  CreateUserRequest,
  CurrentAccountResponse,
  LoginRequest,
  LoginResponse,
} from "@lib/api/generated/model";
import {
  authenticationLogin,
  authenticationLogout,
  authenticationRegister,
  currentAccountCurrent,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { resetClientState } from "@stores/reset-client-state";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { useCallback } from "react";

interface AccountSessionResult {
  account: CurrentAccountResponse;
  session: LoginResponse;
}

async function createAccountSession(
  credentials: LoginRequest,
): Promise<AccountSessionResult> {
  const session = await authenticationLogin(credentials);
  const account = await currentAccountCurrent({
    accessToken: session.accessToken,
  });

  return { account, session };
}

function useStoreAccountSession() {
  const queryClient = useQueryClient();
  const setSession = useAuthStore((state) => state.setSession);

  return useCallback(
    ({ account, session }: AccountSessionResult) => {
      queryClient.removeQueries();
      queryClient.setQueryData(apiQueryKeys.account.current, account);
      setSession(session);
    },
    [queryClient, setSession],
  );
}

export function useLogin() {
  const storeAccountSession = useStoreAccountSession();

  return useMutation({
    mutationKey: apiQueryKeys.authentication.login,
    mutationFn: createAccountSession,
    onSuccess: storeAccountSession,
  });
}

export function useRegister() {
  const storeAccountSession = useStoreAccountSession();

  return useMutation({
    mutationKey: apiQueryKeys.authentication.register,
    mutationFn: async (credentials: CreateUserRequest) => {
      await authenticationRegister(credentials);
      return createAccountSession(credentials);
    },
    onSuccess: storeAccountSession,
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const refreshToken = useAuthStore((state) => state.refreshToken);

  return useCallback(() => {
    // Revoga a sessão de refresh no servidor antes de limpar o estado local; a conexão pode
    // já estar indo embora (navegação), então isso não bloqueia nem precisa de retorno.
    if (refreshToken) {
      authenticationLogout({ refreshToken }).catch(() => {
        // Já estamos saindo; uma falha aqui não impede o logout local.
      });
    }

    resetClientState();
    queryClient.removeQueries();
    navigate({ to: "/" });
  }, [queryClient, navigate, refreshToken]);
}
