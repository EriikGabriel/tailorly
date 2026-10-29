import {
  ANONYMOUS_TOKEN_PREFIX,
  useAnonymousSessionStore,
} from "../../stores/anonymous-session-store";
import { useAuthStore } from "../../stores/auth-store";

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: string[] | null;
  path: string;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly details: string[];
  readonly path: string | null;

  constructor(status: number, response: Partial<ApiErrorResponse>) {
    super(response.message ?? `A API respondeu com status ${status}.`);
    this.name = "ApiClientError";
    this.status = status;
    this.details = response.details ?? [];
    this.path = response.path ?? null;
  }
}

export interface ApiRequestOptions extends RequestInit {
  accessToken?: string;
  language?: string;
  /** Tipo aceito na resposta; JSON por padrão. */
  accept?: string;
}

/** Baixa um arquivo (PDF, Markdown) com as mesmas credenciais e o mesmo tratamento de erro do JSON. */
export async function apiBlob(
  path: string,
  options: ApiRequestOptions = {},
): Promise<Blob> {
  const response = await apiFetch(path, {
    accept: "application/pdf, text/markdown, */*",
    ...options,
  });
  return response.blob();
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const response = await apiFetch(path, options);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function apiFetch(
  path: string,
  {
    accessToken,
    language = "pt-BR",
    accept = "application/json",
    headers,
    ...init
  }: ApiRequestOptions,
): Promise<Response> {
  const requestHeaders = new Headers(headers);
  requestHeaders.set("Accept", accept);
  requestHeaders.set("Accept-Language", language);
  if (accessToken) requestHeaders.set("Authorization", "Bearer " + accessToken);

  const response = await fetch(path, { ...init, headers: requestHeaders });
  if (!response.ok) {
    let error: Partial<ApiErrorResponse> = {};
    try {
      error = (await response.json()) as ApiErrorResponse;
    } catch {
      // Preserve a useful status when a proxy or network layer returns non-JSON.
    }
    // Uma sessão anônima expirada ou revogada não volta a valer: descarta a credencial local.
    if (
      response.status === 401 &&
      accessToken?.startsWith(ANONYMOUS_TOKEN_PREFIX)
    ) {
      const session = useAnonymousSessionStore.getState().session;
      if (session?.token === accessToken) {
        useAnonymousSessionStore.getState().clear();
      }
    }
    // Idem para o JWT da conta (expirou ou a conta deixou de existir): a interface deixa de se
    // mostrar logada, em vez de repetir chamadas que o servidor recusa.
    if (
      response.status === 401 &&
      accessToken &&
      !accessToken.startsWith(ANONYMOUS_TOKEN_PREFIX) &&
      useAuthStore.getState().accessToken === accessToken
    ) {
      useAuthStore.getState().clearSession();
    }
    throw new ApiClientError(response.status, error);
  }

  return response;
}
