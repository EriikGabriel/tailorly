import { ApiClientError } from "./client";

export const SESSION_EXPIRED_MESSAGE =
  "Sua sessão expirou ou não é mais válida. Entre novamente (ou envie o Currículo Base de novo, se estiver como visitante) para continuar.";

/** Texto para exibir ao usuário; um 401 vira uma orientação em vez da resposta crua do servidor. */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiClientError && error.status === 401)
    return SESSION_EXPIRED_MESSAGE;
  return error instanceof Error ? error.message : fallback;
}
