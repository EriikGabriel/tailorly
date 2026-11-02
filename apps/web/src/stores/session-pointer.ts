import type { StoreApi } from "zustand";

/**
 * Guarda em `sessionStorage` só um ponteiro (ids) para um item que vive no servidor, para a seleção
 * sobreviver a um reload da aba. É estado de interface: nunca guarda conteúdo, fatos nem cotas, vale só
 * para a aba e é apagado quando o ponteiro é limpo (inclusive em `resetClientState`).
 */
export function readPointer<T>(
  key: string,
  isValid: (value: unknown) => value is T,
): T | null {
  try {
    const raw = globalThis.sessionStorage?.getItem(key);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    return isValid(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export function persistPointer<S>(
  store: StoreApi<S>,
  key: string,
  pick: (state: S) => unknown,
) {
  store.subscribe((state) => {
    try {
      const value = pick(state);
      if (value == null) globalThis.sessionStorage?.removeItem(key);
      else globalThis.sessionStorage?.setItem(key, JSON.stringify(value));
    } catch {
      // Sem armazenamento disponível a seleção só deixa de sobreviver ao reload.
    }
  });
}
