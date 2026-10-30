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

/**
 * No máximo uma renovação de refresh token por vez: chamadas concorrentes que expiram juntas
 * reaproveitam a mesma promessa em vez de rotacionar o token várias vezes (o backend invalida o
 * token anterior a cada rotação).
 */
let refreshInFlight: Promise<string | null> | null = null;

/** Troca o refresh token guardado por um novo par; `null` se não havia um para tentar ou falhou. */
function refreshAccessToken(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) return Promise.resolve(null);

  if (!refreshInFlight) {
    refreshInFlight = fetch("/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          useAuthStore.getState().clearSession();
          return null;
        }
        const session = (await response.json()) as {
          accessToken: string;
          expiresAt: string;
          refreshToken: string;
        };
        useAuthStore.getState().setSession(session);
        return session.accessToken;
      })
      .catch(() => null)
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

async function apiFetch(
  path: string,
  options: ApiRequestOptions,
  retrying = false,
): Promise<Response> {
  const {
    accessToken,
    language = "pt-BR",
    accept = "application/json",
    headers,
    ...init
  } = options;
  const requestHeaders = new Headers(headers);
  requestHeaders.set("Accept", accept);
  requestHeaders.set("Accept-Language", language);
  if (accessToken) requestHeaders.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(path, { ...init, headers: requestHeaders });
  if (!response.ok) {
    // Um JWT de conta expirado tenta renovar pelo refresh token, uma única vez, antes de desistir.
    if (
      response.status === 401 &&
      !retrying &&
      accessToken &&
      !accessToken.startsWith(ANONYMOUS_TOKEN_PREFIX) &&
      useAuthStore.getState().accessToken === accessToken
    ) {
      const renewedToken = await refreshAccessToken();
      if (renewedToken) {
        return apiFetch(path, { ...options, accessToken: renewedToken }, true);
      }
    }

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
    // Idem para o JWT da conta (a renovação falhou, ou já era o próprio retry): a interface
    // deixa de se mostrar logada, em vez de repetir chamadas que o servidor recusa.
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
