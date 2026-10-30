import { create } from "zustand";
import type { LoginResponse } from "../lib/api/generated/model";

interface AuthState {
  accessToken: string | null;
  expiresAt: string | null;
  refreshToken: string | null;
  setSession: (session: StoredSession) => void;
  clearSession: () => void;
}

type StoredSession = Pick<
  LoginResponse,
  "accessToken" | "expiresAt" | "refreshToken"
>;

const SESSION_KEY = "tailorly:auth-session";

function isValidSession(value: unknown): value is StoredSession {
  if (!value || typeof value !== "object") return false;
  const session = value as Partial<StoredSession>;
  return (
    typeof session.accessToken === "string" &&
    session.accessToken.length > 0 &&
    typeof session.refreshToken === "string" &&
    session.refreshToken.length > 0 &&
    typeof session.expiresAt === "string" &&
    Date.parse(session.expiresAt) > Date.now()
  );
}

export function readStoredSession(): StoredSession | null {
  try {
    const stored = globalThis.sessionStorage?.getItem(SESSION_KEY);
    if (!stored) return null;
    const session: unknown = JSON.parse(stored);
    if (isValidSession(session)) return session;
    globalThis.sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // Browsers may block storage; authentication still works in memory.
  }
  return null;
}

function saveStoredSession(session: StoredSession | null) {
  try {
    if (session) {
      globalThis.sessionStorage?.setItem(SESSION_KEY, JSON.stringify(session));
    } else {
      globalThis.sessionStorage?.removeItem(SESSION_KEY);
    }
  } catch {
    // Browsers may block storage; authentication still works in memory.
  }
}

const initialSession = readStoredSession();

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: initialSession?.accessToken ?? null,
  expiresAt: initialSession?.expiresAt ?? null,
  refreshToken: initialSession?.refreshToken ?? null,
  setSession: (session) => {
    const activeSession = isValidSession(session) ? session : null;
    saveStoredSession(activeSession);
    set({
      accessToken: activeSession?.accessToken ?? null,
      expiresAt: activeSession?.expiresAt ?? null,
      refreshToken: activeSession?.refreshToken ?? null,
    });
  },
  clearSession: () => {
    saveStoredSession(null);
    set({ accessToken: null, expiresAt: null, refreshToken: null });
  },
}));
