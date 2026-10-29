import { useEffect, useState } from "react";
import type { JobUrlCheck } from "@/utils/job-url";

const URL_ERROR_DELAY_MS = 350;

/** Erros e avisos só aparecem depois de uma pausa na digitação; o que é válido aparece na hora. */
export function useSettledUrlCheck(check: JobUrlCheck): JobUrlCheck {
  const [settled, setSettled] = useState(check);
  const { status } = check;
  const message = "message" in check ? check.message : "";

  useEffect(() => {
    if (status === "valid" || status === "empty") {
      setSettled({ status, message } as JobUrlCheck);
      return;
    }
    const timer = setTimeout(
      () => setSettled({ status, message } as JobUrlCheck),
      URL_ERROR_DELAY_MS,
    );
    return () => clearTimeout(timer);
  }, [status, message]);

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
