import { ApiClientError } from "@api/client";
import { previewCvPdf } from "@queries/cv-generation";
import { useCallback, useEffect, useRef, useState } from "react";

export interface CvPreviewState {
  /** URL (blob:) do último PDF compilado com sucesso. */
  url: string | null;
  /** Diagnósticos do compilador quando o código atual não compila. */
  diagnostics: string[];
  error: string | null;
  isCompiling: boolean;
  /** Código que produziu o resultado atual (PDF ou erro); nulo antes da primeira compilação. */
  compiledSource: string | null;
  /** Duração da última compilação concluída, em milissegundos. */
  durationMs: number | null;
}

export interface CvPreview extends CvPreviewState {
  /** Compila já, sem esperar a pausa na digitação (e mesmo que o código não tenha mudado). */
  compileNow: () => void;
}

const DEBOUNCE_MS = 350;

const INITIAL: CvPreviewState = {
  url: null,
  diagnostics: [],
  error: null,
  isCompiling: false,
  compiledSource: null,
  durationMs: null,
};

/**
 * Prévia ao vivo do template: a cada pausa na digitação, o servidor compila o código com o conteúdo
 * da geração e devolve o PDF. Um erro de compilação mantém o último PDF válido na tela e mostra os
 * diagnósticos; uma nova digitação cancela a compilação anterior.
 */
export function useCvPreview(
  generationId: string | null,
  source: string | null,
  token: string | null,
): CvPreview {
  const [state, setState] = useState<CvPreviewState>(INITIAL);
  const [forceTick, setForceTick] = useState(0);
  const urlRef = useRef<string | null>(null);
  // Geração + código do PDF exibido: o mesmo código não é compilado de novo (ex.: desfazer até ele).
  const compiledRef = useRef<string | null>(null);
  const forcedRef = useRef(false);

  // Descarta o PDF ao trocar de geração e libera o último ao sair.
  // biome-ignore lint/correctness/useExhaustiveDependencies: o efeito deve reiniciar a cada geração
  useEffect(() => {
    return () => {
      if (urlRef.current) URL.revokeObjectURL(urlRef.current);
      urlRef.current = null;
      compiledRef.current = null;
      setState(INITIAL);
    };
  }, [generationId]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: `forceTick` só serve para reexecutar o efeito
  useEffect(() => {
    if (!generationId || source === null || !token) return;
    const forced = forcedRef.current;
    forcedRef.current = false;
    const key = `${generationId}\u0000${source}`;
    if (!forced && compiledRef.current === key) {
      setState((current) =>
        current.isCompiling || current.error
          ? { ...current, isCompiling: false, error: null, diagnostics: [] }
          : current,
      );
      return;
    }
    const controller = new AbortController();
    setState((current) => ({ ...current, isCompiling: true }));

    const timer = setTimeout(
      async () => {
        const startedAt = performance.now();
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
          compiledRef.current = key;
          setState({
            url: next,
            diagnostics: [],
            error: null,
            isCompiling: false,
            compiledSource: source,
            durationMs: Math.round(performance.now() - startedAt),
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
            compiledSource: source,
            durationMs: Math.round(performance.now() - startedAt),
          }));
        }
      },
      forced ? 0 : DEBOUNCE_MS,
    );

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [generationId, source, token, forceTick]);

  const compileNow = useCallback(() => {
    forcedRef.current = true;
    setForceTick((tick) => tick + 1);
  }, []);

  return { ...state, compileNow };
}
