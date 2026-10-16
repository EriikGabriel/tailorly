import { create } from "zustand";
import type { LoginResponse } from "../lib/api/generated/model";

interface AuthState {
  accessToken: string | null;
  expiresAt: string | null;
  setSession: (
    session: Pick<LoginResponse, "accessToken" | "expiresAt">,
  ) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: null,
  expiresAt: null,
  setSession: ({ accessToken, expiresAt }) => set({ accessToken, expiresAt }),
  clearSession: () => set({ accessToken: null, expiresAt: null }),
}));
