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
}

export async function apiRequest<T>(
  path: string,
  { accessToken, language = "pt-BR", headers, ...init }: ApiRequestOptions = {},
): Promise<T> {
  const requestHeaders = new Headers(headers);
  requestHeaders.set("Accept", "application/json");
  requestHeaders.set("Accept-Language", language);
  if (accessToken) requestHeaders.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(path, { ...init, headers: requestHeaders });
  if (!response.ok) {
    let error: Partial<ApiErrorResponse> = {};
    try {
      error = (await response.json()) as ApiErrorResponse;
    } catch {
      // Preserve a useful status when a proxy or network layer returns non-JSON.
    }
    throw new ApiClientError(response.status, error);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
