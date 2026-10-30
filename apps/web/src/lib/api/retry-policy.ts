import { ApiClientError } from "./client";

/** Quantas vezes repetir uma consulta que falhou por motivo passageiro. */
const MAX_TRANSIENT_RETRIES = 1;

/**
 * Só vale repetir o que pode dar certo na segunda tentativa: falha de rede (`fetch` lança
 * `TypeError`) e erro 5xx do servidor. Resposta 4xx (sessão inválida, sem permissão, não
 * encontrado, dados recusados, limite de taxa) volta igual — repetir só gasta cota e, no 429,
 * piora o bloqueio. Erros de pré-condição lançados pelo próprio cliente também não se repetem.
 */
export function shouldRetryRequest(
  failureCount: number,
  error: unknown,
): boolean {
  if (failureCount >= MAX_TRANSIENT_RETRIES) return false;
  if (error instanceof ApiClientError) return error.status >= 500;
  return error instanceof TypeError;
}
