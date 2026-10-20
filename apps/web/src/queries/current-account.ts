import { ApiClientError } from "@lib/api/client";
import { currentAccountCurrent } from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { queryOptions, skipToken, useQuery } from "@tanstack/react-query";

export function currentAccountQueryOptions(
  accessToken: string | null | undefined,
) {
  return queryOptions({
    queryKey: apiQueryKeys.account.current,
    queryFn: accessToken
      ? async ({ signal }) => {
          try {
            return await currentAccountCurrent({ accessToken, signal });
          } catch (error) {
            if (
              error instanceof ApiClientError &&
              error.status === 401 &&
              useAuthStore.getState().accessToken === accessToken
            ) {
              useAuthStore.getState().clearSession();
            }
            throw error;
          }
        }
      : skipToken,
    staleTime: 60_000,
  });
}

export function useCurrentAccount() {
  const accessToken = useAuthStore((state) => state.accessToken);
  return useQuery(currentAccountQueryOptions(accessToken));
}

export function useCurrentOwnerId() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery({
    ...currentAccountQueryOptions(accessToken),
    select: (account) => account.owner.id,
  });
}
