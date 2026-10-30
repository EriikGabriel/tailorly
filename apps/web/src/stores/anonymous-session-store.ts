import { create } from "zustand";

export interface AnonymousSession {
  token: string;
  sessionId: string;
  ownerId: string;
  expiresAt: string;
}

interface AnonymousSessionState {
  session: AnonymousSession | null;
  setSession: (session: AnonymousSession) => void;
  clear: () => void;
}

export const ANONYMOUS_TOKEN_PREFIX = "tlr_anon_";

/** A sessão ainda vale? Uma sessão vencida só levaria um 401 do servidor. */
export function isAnonymousSessionActive(
  session: AnonymousSession | null | undefined,
  now = Date.now(),
): session is AnonymousSession {
  return session != null && Date.parse(session.expiresAt) > now;
}

const SESSION_KEY = "tailorly:anonymous-session";

function isValidSession(value: unknown): value is AnonymousSession {
  if (!value || typeof value !== "object") return false;
  const session = value as Partial<AnonymousSession>;
  return (
    typeof session.token === "string" &&
    session.token.startsWith(ANONYMOUS_TOKEN_PREFIX) &&
    typeof session.sessionId === "string" &&
    typeof session.ownerId === "string" &&
    typeof session.expiresAt === "string" &&
    Date.parse(session.expiresAt) > Date.now()
  );
}

// Só a credencial da sessão fica no navegador; fatos, cotas e dados do
// currículo continuam no servidor, que é a fonte de verdade.
function readStoredSession(): AnonymousSession | null {
  try {
    const stored = globalThis.sessionStorage?.getItem(SESSION_KEY);
    if (!stored) return null;
    const session: unknown = JSON.parse(stored);
    if (isValidSession(session)) return session;
    globalThis.sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // O navegador pode bloquear o storage; a sessão segue válida em memória.
  }
  return null;
}

function saveStoredSession(session: AnonymousSession | null) {
  try {
    if (session) {
      globalThis.sessionStorage?.setItem(SESSION_KEY, JSON.stringify(session));
    } else {
      globalThis.sessionStorage?.removeItem(SESSION_KEY);
    }
  } catch {
    // O navegador pode bloquear o storage; a sessão segue válida em memória.
  }
}

/**
 * Token da sessão anônima, só se ainda válida. Uma sessão vencida é descartada aqui mesmo, antes de
 * qualquer chamada, em vez de esperar o 401 do servidor.
 */
export function activeAnonymousToken(): string | null {
  const { session, clear } = useAnonymousSessionStore.getState();
  if (!session) return null;
  if (isAnonymousSessionActive(session)) return session.token;
  clear();
  return null;
}

export const useAnonymousSessionStore = create<AnonymousSessionState>()(
  (set) => ({
    session: readStoredSession(),
    setSession: (session) => {
      saveStoredSession(session);
      set({ session });
    },
    clear: () => {
      saveStoredSession(null);
      set({ session: null });
    },
  }),
);
