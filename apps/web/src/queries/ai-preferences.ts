import { aiPreferencesCurrent } from "@lib/api/generated/tailorly";
import { apiQueryKeys } from "@lib/api/query-keys";
import { useAuthStore } from "@stores/auth-store";
import { queryOptions, skipToken, useQuery } from "@tanstack/react-query";

export function aiPreferencesQueryOptions(
  accessToken: string | null | undefined,
) {
  return queryOptions({
    queryKey: apiQueryKeys.aiPreferences.current,
    queryFn: accessToken
      ? ({ signal }: { signal: AbortSignal }) =>
          aiPreferencesCurrent({ accessToken, signal })
      : skipToken,
    staleTime: 60_000,
  });
}

export function useAiPreferences() {
  const accessToken = useAuthStore((state) => state.accessToken);
  return useQuery(aiPreferencesQueryOptions(accessToken));
}
