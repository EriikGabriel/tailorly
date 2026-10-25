import { useCallback, useState } from "react";

export type DetectedOllamaModel = { name: string; sizeLabel?: string };

type OllamaDetectionState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; models: DetectedOllamaModel[] }
  | { status: "error"; message: string };

// Ollama é um processo local do próprio usuário: a detecção acontece direto no
// navegador contra o daemon na máquina da pessoa, nunca passando pelo backend.
const OLLAMA_TAGS_ENDPOINT = "http://127.0.0.1:11434/api/tags";
const DETECTION_TIMEOUT_MS = 2500;

function formatSize(bytes: number | undefined): string | undefined {
  if (!bytes) return undefined;
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`;
}

export function useOllamaModels() {
  const [state, setState] = useState<OllamaDetectionState>({
    status: "idle",
  });

  const detect = useCallback(async () => {
    setState({ status: "loading" });

    const controller = new AbortController();
    const timeout = window.setTimeout(
      () => controller.abort(),
      DETECTION_TIMEOUT_MS,
    );

    try {
      const response = await fetch(OLLAMA_TAGS_ENDPOINT, {
        signal: controller.signal,
      });
      if (!response.ok) {
        throw new Error(`Ollama respondeu com status ${response.status}`);
      }

      const data = (await response.json()) as {
        models?: { name: string; size?: number }[];
      };

      setState({
        status: "success",
        models: (data.models ?? []).map((model) => ({
          name: model.name,
          sizeLabel: formatSize(model.size),
        })),
      });
    } catch {
      setState({
        status: "error",
        message:
          "Não foi possível detectar o Ollama em http://127.0.0.1:11434 neste dispositivo. Verifique se ele está instalado e em execução.",
      });
    } finally {
      window.clearTimeout(timeout);
    }
  }, []);

  return { state, detect };
}
