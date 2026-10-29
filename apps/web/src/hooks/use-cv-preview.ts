import { ApiClientError } from "@lib/api/client";
import { previewCvPdf } from "@queries/cv-generation";
import { useEffect, useRef, useState } from "react";

export interface CvPreviewState {
  /** URL (blob:) do último PDF compilado com sucesso. */
  url: string | null;
  /** Diagnósticos do compilador quando o código atual não compila. */
  diagnostics: string[];
  error: string | null;
  isCompiling: boolean;
}

const DEBOUNCE_MS = 600;

/**
 * Prévia ao vivo do template: a cada pausa na digitação, o servidor compila o código com o conteúdo
 * da geração e devolve o PDF. Um erro de compilação mantém o último PDF válido na tela e mostra os
 * diagnósticos; uma nova digitação cancela a compilação anterior.
 */
export function useCvPreview(
  generationId: string | null,
  source: string | null,
  token: string | null,
): CvPreviewState {
  const [state, setState] = useState<CvPreviewState>({
    url: null,
    diagnostics: [],
    error: null,
    isCompiling: false,
  });
  const urlRef = useRef<string | null>(null);

  // Descarta o PDF ao trocar de geração e libera o último ao sair.
  // biome-ignore lint/correctness/useExhaustiveDependencies: o efeito deve reiniciar a cada geração
  useEffect(() => {
    return () => {
      if (urlRef.current) URL.revokeObjectURL(urlRef.current);
      urlRef.current = null;
      setState({ url: null, diagnostics: [], error: null, isCompiling: false });
    };
  }, [generationId]);

  useEffect(() => {
    if (!generationId || source === null || !token) return;
    const controller = new AbortController();
    setState((current) => ({ ...current, isCompiling: true }));

    const timer = setTimeout(async () => {
      try {
        const blob = await previewCvPdf(
          generationId,
          source,
          token,
          controller.signal,
        );
        const next = URL.createObjectURL(blob);
        if (urlRef.current) URL.revokeObjectURL(urlRef.current);
        urlRef.current = next;
        setState({
          url: next,
          diagnostics: [],
          error: null,
          isCompiling: false,
        });
      } catch (cause) {
        if (controller.signal.aborted) return;
        setState((current) => ({
          ...current,
          diagnostics: cause instanceof ApiClientError ? cause.details : [],
          error:
            cause instanceof Error
              ? cause.message
              : "Não foi possível compilar o template.",
          isCompiling: false,
        }));
      }
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [generationId, source, token]);

  return state;
}
