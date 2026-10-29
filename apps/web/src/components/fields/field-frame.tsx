import { Tooltip, TooltipPanel, TooltipTrigger } from "@animate/base/tooltip";
import { BadgeCheck, Info } from "@react-zero-ui/icon-sprite";
import { cn } from "cn";
import type { ReactNode } from "react";

interface FieldFrameProps {
  id: string;
  label: string;
  hint?: string;
  wide?: boolean;
  children: ReactNode;
  className?: string;
  /** Aviso exibido abaixo do controle (ex.: pendência de confirmação). */
  status?: ReactNode;
  highlight?: string;
  /** Fato confirmado ou corrigido pela pessoa; mostra o selo ao lado do rótulo. */
  verified?: boolean;
}

/** Rótulo + ajuda em tooltip (também lida por leitores de tela) + controle. */
export function FieldFrame({
  id,
  label,
  hint,
  wide,
  children,
  className,
  status,
  highlight,
  verified,
}: FieldFrameProps) {
  return (
    <div
      className={cn(
        "min-w-0 space-y-1.5",
        wide && "sm:col-span-2",
        highlight,
        className,
      )}
    >
      <div className="flex items-center gap-1.5">
        <label
          htmlFor={id}
          className="text-xs font-semibold tracking-[0.4px] text-on-surface-variant uppercase"
        >
          {label}
        </label>
        {verified && (
          <span
            role="img"
            aria-label="Fato verificado"
            title="Fato verificado"
            className="text-tertiary-700"
          >
            <BadgeCheck aria-hidden="true" className="size-4" />
          </span>
        )}
        {hint && (
          <Tooltip>
            <TooltipTrigger
              type="button"
              aria-label={`Ajuda: ${label}`}
              className="rounded-full text-outline outline-none hover:text-primary-900 focus-visible:ring-2 focus-visible:ring-ring"
            >
              <Info aria-hidden="true" className="size-3.5" />
            </TooltipTrigger>
            <TooltipPanel>{hint}</TooltipPanel>
          </Tooltip>
        )}
      </div>
      {children}
      {status}
      {hint && (
        <span id={`${id}-hint`} className="sr-only">
          {hint}
        </span>
      )}
    </div>
  );
}
