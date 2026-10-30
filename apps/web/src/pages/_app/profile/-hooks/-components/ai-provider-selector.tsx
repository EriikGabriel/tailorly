import {
  Bot,
  CircleCheck,
  HardDrive,
  ListChecks,
  Workflow,
} from "@react-zero-ui/icon-sprite";
import { motion, useReducedMotion } from "motion/react";
import type { ComponentType } from "react";
import {
  type InterpretationProvider,
  interpretationProviderOptions,
} from "@/constants/ai-providers";

const providerIcons: Record<
  InterpretationProvider,
  ComponentType<{ className?: string }>
> = {
  "remote-with-ollama-fallback": Workflow,
  remote: Bot,
  "ollama-only": HardDrive,
  rules: ListChecks,
};

type AiProviderSelectorProps = {
  value: InterpretationProvider;
  onChange: (value: InterpretationProvider) => void;
};

export function AiProviderSelector({
  value,
  onChange,
}: AiProviderSelectorProps) {
  const reduceMotion = useReducedMotion();

  return (
    <div
      role="radiogroup"
      aria-label="Provedor de IA para extração do Currículo Base"
      className="grid gap-2 sm:grid-cols-2"
    >
      {interpretationProviderOptions.map((provider) => {
        const Icon = providerIcons[provider.value];
        const selected = value === provider.value;

        return (
          // biome-ignore lint/a11y/useSemanticElements: rich card content can't live inside a native <input type="radio">.
          <button
            key={provider.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(provider.value)}
            className={`relative flex flex-col gap-1 rounded-xl p-2.5 text-left transition-colors ${
              selected
                ? "bg-surface-container-low"
                : "bg-card hover:bg-surface-container-low/60"
            }`}
          >
            <motion.span
              aria-hidden="true"
              className="pointer-events-none absolute inset-0 rounded-xl border-2"
              initial={false}
              animate={{
                borderColor: selected
                  ? "var(--color-primary)"
                  : "var(--color-outline-variant)",
              }}
              transition={
                reduceMotion
                  ? { duration: 0 }
                  : { type: "spring", stiffness: 400, damping: 30 }
              }
            />
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span
                  className={`flex size-7 shrink-0 items-center justify-center rounded-lg ${
                    selected
                      ? "bg-primary text-primary-foreground"
                      : "bg-surface-container text-on-surface-variant"
                  }`}
                >
                  <Icon className="size-3.5" />
                </span>
                <span className="text-label-md font-semibold text-primary-950">
                  {provider.name}
                </span>
              </div>
              <span
                aria-hidden="true"
                className={`size-3 shrink-0 rounded-full ${
                  selected
                    ? "bg-on-secondary-container"
                    : "border border-outline"
                }`}
              />
            </div>
            <p className="text-body-sm text-on-surface-variant">
              {provider.description}
            </p>
            {provider.recommended && (
              <span
                className={`inline-flex items-center gap-1 text-label-sm font-semibold ${
                  selected
                    ? "text-on-secondary-container"
                    : "text-on-surface-variant"
                }`}
              >
                <CircleCheck aria-hidden="true" className="size-3" />
                Recomendado (resiliência automática)
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
