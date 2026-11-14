import { BadgeCheck } from "@react-zero-ui/icon-sprite";
import type { JobUrlCheck } from "@utils/job/job-url";
import { FULLY_COMPATIBLE_PLATFORMS } from "@utils/job/job-url";
import { useEffect, useState } from "react";

const URL_ERROR_DELAY_MS = 350;

/** Erros e avisos só aparecem depois de uma pausa na digitação; o que é válido aparece na hora. */
export function useSettledUrlCheck(check: JobUrlCheck): JobUrlCheck {
  const [settled, setSettled] = useState(check);
  const { status } = check;
  const message = "message" in check ? check.message : "";
  const platform = check.status === "valid" ? check.platform : undefined;
  const fullyCompatible =
    check.status === "valid" ? check.fullyCompatible : undefined;

  useEffect(() => {
    const next = { status, message, platform, fullyCompatible } as JobUrlCheck;
    if (status === "valid" || status === "empty") {
      setSettled(next);
      return;
    }
    const timer = setTimeout(() => setSettled(next), URL_ERROR_DELAY_MS);
    return () => clearTimeout(timer);
  }, [status, message, platform, fullyCompatible]);

  return settled;
}

const tones = {
  empty: "text-on-surface-variant",
  invalid: "text-error",
  warning: "text-on-surface-variant",
  valid: "text-on-tertiary-container",
} as const;

export function JobUrlFeedback({
  id,
  check,
  className = "",
}: {
  id: string;
  check: JobUrlCheck;
  className?: string;
}) {
  return (
    <p
      id={id}
      aria-live="polite"
      className={`min-h-5 text-[13px] leading-5 ${tones[check.status]} ${className}`}
    >
      {check.status !== "empty" && check.message}
    </p>
  );
}

/** Ícone do campo de link: selo de verificação quando a plataforma é totalmente compatível. */
export function JobUrlBadge({
  check,
  fallback,
  className,
}: {
  check: JobUrlCheck;
  fallback: React.ReactNode;
  className: string;
}) {
  if (check.status === "valid" && check.fullyCompatible)
    return (
      <BadgeCheck
        role="img"
        aria-label={`Plataforma totalmente compatível: ${check.platform}`}
        className={`${className} text-on-tertiary-container`}
      />
    );
  return fallback;
}

/** Lista explícita das plataformas com leitura por link validada. */
export function CompatiblePlatforms({
  className = "",
}: {
  className?: string;
}) {
  return (
    <p
      className={`flex flex-wrap items-center gap-x-2 gap-y-1 text-xs tracking-[0.3px] text-on-surface-variant ${className}`}
    >
      <span className="font-medium">Totalmente compatíveis:</span>
      {FULLY_COMPATIBLE_PLATFORMS.map(({ name }) => (
        <span
          key={name}
          className="inline-flex items-center gap-1 font-semibold text-on-tertiary-container"
        >
          <BadgeCheck aria-hidden="true" className="size-3.5" />
          {name}
        </span>
      ))}
    </p>
  );
}
