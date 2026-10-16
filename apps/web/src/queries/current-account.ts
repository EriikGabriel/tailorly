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
      ? ({ signal }) => currentAccountCurrent({ accessToken, signal })
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
