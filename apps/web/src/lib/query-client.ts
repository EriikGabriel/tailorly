import { QueryClient } from "@tanstack/react-query";
import { shouldRetryRequest } from "./api/retry-policy";

/**
 * Padrões que evitam chamadas desnecessárias: dados ficam "frescos" por 30 s (voltar a uma tela não
 * refaz a consulta), não há refetch ao focar a janela (as mutações já invalidam o que mudou) e só
 * falhas passageiras são repetidas. Consultas que precisam de outro comportamento (polling,
 * `staleTime` infinito) declaram isso no próprio hook.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: shouldRetryRequest,
    },
  },
});
