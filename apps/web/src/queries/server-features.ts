import { apiRequest } from "@api/client";
import { apiQueryKeys } from "@api/query-keys";
import { useQuery } from "@tanstack/react-query";

/**
 * Recursos opcionais que o servidor realmente oferece (`GET /v1/features`, público). A interface
 * esconde o que o ambiente não tem, em vez de mostrar controles que nunca funcionariam (por exemplo,
 * vault Obsidian e busca web em produção sem essa configuração). Chamada escrita à mão porque o
 * cliente gerado ainda não inclui esta rota.
 */
export interface ServerFeatures {
  /** O servidor tem uma raiz de vaults Obsidian (instalação local). */
  vault: boolean;
  /** O servidor tem um provedor de busca web (SearXNG local ou Tavily). */
  webSearch: boolean;
}

/** Enquanto carrega, ou se a consulta falhar, nada opcional é mostrado. */
const NO_FEATURES: ServerFeatures = { vault: false, webSearch: false };

export function useServerFeatures(): ServerFeatures {
  const query = useQuery({
    queryKey: apiQueryKeys.serverFeatures.all,
    queryFn: () => apiRequest<ServerFeatures>("/v1/features"),
    // A configuração só muda com um novo deploy.
    staleTime: 10 * 60_000,
  });
  return query.data ?? NO_FEATURES;
}
