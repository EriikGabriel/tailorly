import {
  AlertTriangle,
  Bot,
  Brain,
  Cable,
  CircleCheck,
  Eye,
  EyeOff,
  Lock,
  Telescope,
  Waypoints,
} from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";
import { useMemo, useState } from "react";
import {
  previewInterpretationSettings,
  type RemoteProviderVendor,
  remoteProviderVendors,
} from "@/data/profile-preview";
import { FieldLabel } from "./section-card";

const vendorIcons: Record<
  RemoteProviderVendor,
  ComponentType<{ className?: string }>
> = {
  openai: Bot,
  deepseek: Telescope,
  openrouter: Waypoints,
  anthropic: Brain,
  custom: Cable,
};

function isValidUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

export function AiConnectionParametersCard() {
  const [config, setConfig] = useState(previewInterpretationSettings.remote);
  const [revealKey, setRevealKey] = useState(false);

  const activeVendor =
    remoteProviderVendors.find((vendor) => vendor.endpoint === config.endpoint)
      ?.id ?? "custom";

  const validation = useMemo(
    () => ({
      endpoint: isValidUrl(config.endpoint),
      model: config.model.trim().length > 0,
      apiKey: config.apiKey.trim().length > 0,
    }),
    [config],
  );
  const isValid = validation.endpoint && validation.model && validation.apiKey;
  const modelPlaceholder =
    remoteProviderVendors.find((vendor) => vendor.id === activeVendor)
      ?.modelPlaceholder ?? "ex.: modelo do seu provedor";

  function selectVendor(vendorId: RemoteProviderVendor) {
    const vendor = remoteProviderVendors.find((item) => item.id === vendorId);
    if (!vendor) return;
    setConfig((prev) => ({
      ...prev,
      endpoint: vendor.endpoint || prev.endpoint,
    }));
  }

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant pb-2.5">
        <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
          <Bot aria-hidden="true" className="size-4" />
          Provedor Remoto Principal — Interpretação do Currículo Base
        </h3>
        <span className="rounded-full bg-card px-2.5 py-0.5 text-label-sm font-medium text-on-secondary-container">
          tailorly.interpretation.remote
        </span>
      </div>

      <div className="flex flex-col gap-1.5">
        <FieldLabel>Provedor</FieldLabel>
        <div
          className="flex flex-wrap gap-1.5"
          role="radiogroup"
          aria-label="Provedor remoto"
        >
          {remoteProviderVendors.map((vendor) => {
            const VendorIcon = vendorIcons[vendor.id];
            const selected = activeVendor === vendor.id;
            return (
              // biome-ignore lint/a11y/useSemanticElements: chip with icon + label can't live inside a native <input type="radio">.
              <button
                key={vendor.id}
                type="button"
                role="radio"
                aria-checked={selected}
                onClick={() => selectVendor(vendor.id)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-label-sm font-semibold transition-colors ${
                  selected
                    ? "bg-primary text-primary-foreground"
                    : "bg-card text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                <VendorIcon className="size-3.5" />
                {vendor.name}
              </button>
            );
          })}
        </div>
        <p className="text-body-sm text-on-surface-variant">
          O backend guarda um único slot de provedor remoto (
          <span className="font-mono">tailorly.interpretation.remote.*</span>
          ), mas qualquer serviço compatível com o formato Chat Completions
          funciona aqui — inclusive Anthropic, DeepSeek e OpenRouter, via suas
          próprias camadas de compatibilidade OpenAI.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <FieldLabel
            trailing={
              <ValidationHint ok={validation.endpoint} label="Endpoint" />
            }
          >
            Endpoint (compatível com Chat Completions)
          </FieldLabel>
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
          <FieldLabel
            trailing={<ValidationHint ok={validation.model} label="Modelo" />}
          >
            Modelo (TAILORLY_REMOTE_MODEL)
          </FieldLabel>
          <input
            placeholder={modelPlaceholder}
            value={config.model}
            onChange={(event) =>
              setConfig((prev) => ({ ...prev, model: event.target.value }))
            }
            className="w-full rounded-lg bg-card px-4 py-2.5 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </div>

        <div className="flex flex-col gap-1.5 sm:col-span-2">
          <FieldLabel
            trailing={
              <span className="inline-flex items-center gap-1 text-label-sm font-semibold text-on-secondary-container">
                <Lock aria-hidden="true" className="size-3" />
                Nunca enviada ao backend sem TLS
              </span>
            }
          >
            Chave de API (TAILORLY_REMOTE_API_KEY)
          </FieldLabel>
          <div className="relative">
            <input
              type={revealKey ? "text" : "password"}
              placeholder="sk-..."
              value={config.apiKey}
              onChange={(event) =>
                setConfig((prev) => ({
                  ...prev,
                  apiKey: event.target.value,
                }))
              }
              className="w-full rounded-lg bg-card py-2.5 pr-24 pl-4 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            />
            <button
              type="button"
              onClick={() => setRevealKey((prev) => !prev)}
              className="absolute top-1.5 right-2 inline-flex items-center gap-1 rounded px-2.5 py-1 text-label-sm font-semibold text-primary-950 hover:bg-surface-container"
            >
              {revealKey ? (
                <EyeOff aria-hidden="true" className="size-3.5" />
              ) : (
                <Eye aria-hidden="true" className="size-3.5" />
              )}
              {revealKey ? "Ocultar" : "Mostrar"}
            </button>
          </div>
        </div>
      </div>

      <p className="text-body-sm text-on-surface-variant">
        {isValid
          ? "Configuração completa. Essas credenciais ficam no backend como variáveis de ambiente (TAILORLY_REMOTE_*); a persistência por conta ainda está planejada."
          : "Preencha endpoint, modelo e chave de API para habilitar a extração via este provedor."}
      </p>
    </div>
  );
}

function ValidationHint({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 text-label-sm font-semibold ${
        ok ? "text-on-secondary-container" : "text-on-surface-variant"
      }`}
    >
      {ok ? (
        <CircleCheck aria-hidden="true" className="size-3" />
      ) : (
        <AlertTriangle aria-hidden="true" className="size-3" />
      )}
      {label}
    </span>
  );
}
