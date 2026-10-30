import { anonymousSessionCreate } from "@lib/api/generated/tailorly";
import { pickToken } from "@stores/active-token";
import {
  activeAnonymousToken,
  isAnonymousSessionActive,
  useAnonymousSessionStore,
} from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";
import { type OwnerKind, useBaseCvStore } from "@stores/base-cv-store";
import type { QueryClient } from "@tanstack/react-query";
import { currentAccountQueryOptions } from "./current-account";

export interface ApiCredential {
  kind: OwnerKind;
  token: string;
  ownerId: string;
}

/** Token que acessa os dados do Currículo Base atual (conta ou sessão anônima). */
export function useApiToken(): string | null {
  const accountToken = useAuthStore((state) => state.accessToken);
  const anonymousToken = useAnonymousSessionStore((state) =>
    isAnonymousSessionActive(state.session) ? state.session.token : null,
  );
  const ownerKind = useBaseCvStore((state) => state.ownerKind);
  return pickToken({ accountToken, anonymousToken, ownerKind });
}

let pendingAnonymousSession: Promise<ApiCredential> | null = null;

// Evita emitir várias sessões quando duas chamadas pedem a credencial ao mesmo tempo.
function ensureAnonymousSession(): Promise<ApiCredential> {
  // Sessão vencida não é reaproveitada: emite uma nova em vez de gastar uma chamada que daria 401.
  const stored = useAnonymousSessionStore.getState().session;
  const existing = isAnonymousSessionActive(stored) ? stored : null;
  if (existing) {
    return Promise.resolve({
      kind: "anonymous",
      token: existing.token,
      ownerId: existing.ownerId,
    });
  }

  pendingAnonymousSession ??= anonymousSessionCreate()
    .then((issued) => {
      useAnonymousSessionStore.getState().setSession({
        token: issued.token,
        sessionId: issued.sessionId,
        ownerId: issued.ownerId,
        expiresAt: issued.expiresAt,
      });
      return {
        kind: "anonymous" as const,
        token: issued.token,
        ownerId: issued.ownerId,
      };
    })
    .finally(() => {
      pendingAnonymousSession = null;
    });
  return pendingAnonymousSession;
}

/**
 * Credencial para um novo envio: a conta, se houver login; senão uma sessão anônima,
 * emitida pelo servidor sob demanda (nunca antes de a pessoa enviar um arquivo).
 */
export async function ensureCredential(
  queryClient: QueryClient,
): Promise<ApiCredential> {
  const accountToken = useAuthStore.getState().accessToken;
  if (accountToken) {
    const account = await queryClient.fetchQuery(
      currentAccountQueryOptions(accountToken),
    );
    return { kind: "account", token: accountToken, ownerId: account.owner.id };
  }
  return ensureAnonymousSession();
}

/** Credencial do dono de um recurso, sem alternativa: cair no outro token acessaria o owner errado (403). */
export function tokenForOwnerKind(ownerKind: OwnerKind | null): string | null {
  if (ownerKind === "account") return useAuthStore.getState().accessToken;
  if (ownerKind === "anonymous") return activeAnonymousToken();
  return null;
}
