import { type Notice, notify } from "@stores/notification-store";
import type { QueryClient } from "@tanstack/react-query";

/**
 * Como uma mutação avisa que terminou. `success`/`error` devolvem o aviso, ou `null` para não avisar.
 * Declarada no `meta` da mutação, para o texto ficar junto de quem conhece o resultado.
 */
export interface MutationNotifyMeta {
  success?: (data: never, variables: never) => Notice | null;
  error?: (error: Error, variables: never) => Notice | null;
}

declare module "@tanstack/react-query" {
  interface Register {
    mutationMeta: { notify?: MutationNotifyMeta };
  }
}

/** Liga o cache de mutações às notificações; chamado uma vez ao criar o `QueryClient`. */
export function connectMutationNotifications(client: QueryClient) {
  return client.getMutationCache().subscribe((event) => {
    if (event.type !== "updated") return;
    const { mutation, action } = event;
    const meta = mutation.meta?.notify;
    if (!meta) return;

    if (action.type === "success") {
      const notice = (
        meta.success as
          | ((data: unknown, variables: unknown) => Notice | null)
          | undefined
      )?.(action.data, mutation.state.variables);
      if (notice) notify(notice);
    } else if (action.type === "error") {
      const notice = (
        meta.error as
          | ((error: Error, variables: unknown) => Notice | null)
          | undefined
      )?.(action.error as Error, mutation.state.variables);
      if (notice) notify(notice);
    }
  });
}
