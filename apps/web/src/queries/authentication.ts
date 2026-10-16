import type {
  CreateUserRequest,
  CurrentAccountResponse,
  LoginRequest,
  LoginResponse,
} from "@lib/api/generated/model";
import {
  authenticationLogin,
  authenticationRegister,
  currentAccountCurrent,
} from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { resetClientState } from "@stores/reset-client-state";
import { useMutation, useQueryClient } from "@tanstack/react-query";
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

  return useCallback(() => {
    resetClientState();
    queryClient.removeQueries();
  }, [queryClient]);
}
