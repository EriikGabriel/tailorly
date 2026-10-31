/** Limites e passos do divisor entre o editor e a prévia (proporção ocupada pelo primeiro painel). */
export const MIN_RATIO = 0.25;
export const MAX_RATIO = 0.75;
export const DEFAULT_RATIO = 0.5;
export const KEY_STEP = 0.02;
export const KEY_STEP_LARGE = 0.1;

/** Mantém a proporção dentro dos limites; valores inválidos voltam ao padrão. */
export function clampRatio(
  value: number,
  min = MIN_RATIO,
  max = MAX_RATIO,
): number {
  if (!Number.isFinite(value)) return DEFAULT_RATIO;
  return Math.min(max, Math.max(min, value));
}

/** Proporção do primeiro painel para a posição do ponteiro dentro do contêiner. */
export function ratioFromPointer(
  clientX: number,
  left: number,
  width: number,
  min = MIN_RATIO,
  max = MAX_RATIO,
): number {
  if (width <= 0) return DEFAULT_RATIO;
  return clampRatio((clientX - left) / width, min, max);
}

/** Nova proporção para uma tecla do divisor, ou `null` quando a tecla não o move. */
export function ratioForKey(
  key: string,
  current: number,
  shift: boolean,
  min = MIN_RATIO,
  max = MAX_RATIO,
): number | null {
  const step = shift ? KEY_STEP_LARGE : KEY_STEP;
  switch (key) {
    case "ArrowLeft":
      return clampRatio(current - step, min, max);
    case "ArrowRight":
      return clampRatio(current + step, min, max);
    case "Home":
      return min;
    case "End":
      return max;
    default:
      return null;
  }
}

/** Lê a proporção guardada no navegador; texto ausente ou inválido vira `null`. */
export function parseStoredRatio(raw: string | null): number | null {
  if (raw === null) return null;
  const value = Number.parseFloat(raw);
  return Number.isFinite(value) ? clampRatio(value) : null;
}
