import {
  AlertTriangle,
  HardDrive,
  RefreshCw,
  Server,
} from "@react-zero-ui/icon-sprite";
import { useState } from "react";
import { previewInterpretationSettings } from "@/data/profile-preview";
import { FieldLabel } from "./section-card";
import { useOllamaModels } from "./use-ollama-models";

type AiFallbackCardProps = {
  mode: "fallback" | "primary";
};

export function AiFallbackCard({ mode }: AiFallbackCardProps) {
  const [config, setConfig] = useState(previewInterpretationSettings.ollama);
  const { state, detect } = useOllamaModels();

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-card">
            <Server
              aria-hidden="true"
              className="size-4 text-on-surface-variant"
            />
          </span>
          <div>
            <h3 className="text-title-md font-semibold text-primary-950">
              {mode === "primary"
                ? "Ollama Local — Único Provedor de Extração"
                : "Ollama Local — Fallback de Extração"}
            </h3>
            <p className="text-body-sm text-on-surface-variant">
              {mode === "primary"
                ? "Toda a extração roda neste modelo local; nenhuma chamada sai do seu dispositivo. Se o Ollama não estiver em execução ou o modelo não existir, a extração falha — não há fallback automático para um provedor remoto nem para regras."
                : "Usado automaticamente quando a chamada ao provedor remoto falhar (RNF07). Se os dois falharem, a extração não é concluída — não há fallback silencioso para regras."}
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 pt-1 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <FieldLabel>Endpoint (TAILORLY_OLLAMA_ENDPOINT)</FieldLabel>
          <input
            type="url"
            value={config.endpoint}
            onChange={(event) =>
              setConfig((prev) => ({ ...prev, endpoint: event.target.value }))
            }
            className="w-full rounded-lg bg-card px-4 py-2.5 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <FieldLabel>Modelo local (TAILORLY_OLLAMA_MODEL)</FieldLabel>
          <input
            value={config.model}
            onChange={(event) =>
              setConfig((prev) => ({ ...prev, model: event.target.value }))
            }
            className="w-full rounded-lg bg-card px-4 py-2.5 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </div>
      </div>

      <div className="flex flex-col gap-2 rounded-lg bg-card p-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="flex items-center gap-1.5 text-label-md font-semibold text-primary-950">
            <HardDrive aria-hidden="true" className="size-3.5" />
            Modelos instalados neste dispositivo
          </p>
          <button
            type="button"
            onClick={detect}
            disabled={state.status === "loading"}
            className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-label-sm font-semibold text-on-secondary-container hover:bg-surface-container-low disabled:opacity-60"
          >
            <RefreshCw
              aria-hidden="true"
              className={`size-3.5 ${state.status === "loading" ? "animate-spin" : ""}`}
            />
            {state.status === "loading" ? "Detectando..." : "Detectar"}
          </button>
        </div>

        {state.status === "idle" && (
          <p className="text-body-sm text-on-surface-variant">
            Clique em &quot;Detectar&quot; para consultar o Ollama em{" "}
            <span className="font-mono text-primary-950">
              {previewInterpretationSettings.ollama.endpoint.replace(
                "/v1/chat/completions",
                "",
              )}
            </span>{" "}
            e listar os modelos realmente baixados neste computador.
          </p>
        )}

        {state.status === "error" && (
          <p className="flex items-start gap-1.5 text-body-sm text-destructive">
            <AlertTriangle
              aria-hidden="true"
              className="mt-0.5 size-3.5 shrink-0"
            />
            {state.message}
          </p>
        )}

        {state.status === "success" && state.models.length === 0 && (
          <p className="text-body-sm text-on-surface-variant">
            Ollama respondeu, mas nenhum modelo foi baixado ainda. Rode{" "}
            <span className="font-mono text-primary-950">
              ollama pull {config.model}
            </span>{" "}
            para habilitar este modo.
          </p>
        )}

        {state.status === "success" && state.models.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {state.models.map((model) => (
              <button
                key={model.name}
                type="button"
                onClick={() =>
                  setConfig((prev) => ({ ...prev, model: model.name }))
                }
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-label-sm font-semibold transition-colors ${
                  config.model === model.name
                    ? "bg-primary text-primary-foreground"
                    : "bg-surface-container-low text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                {model.name}
                {model.sizeLabel && (
                  <span className="opacity-70">({model.sizeLabel})</span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
