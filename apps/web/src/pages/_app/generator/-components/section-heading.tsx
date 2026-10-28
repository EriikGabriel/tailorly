import type { ReactNode } from "react";

export function SectionHeading({
  step,
  title,
  trailing,
  tone = "primary",
}: {
  step: number;
  title: string;
  trailing?: ReactNode;
  tone?: "primary" | "muted";
}) {
  return (
    <div className="flex w-full items-center justify-between gap-3">
      <div className="flex items-center gap-2">
        <span
          className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white ${
            tone === "primary" ? "bg-primary-950" : "bg-secondary-700"
          }`}
        >
          {step}
        </span>
        <h3 className="text-base font-semibold leading-6 text-on-surface">
          {title}
        </h3>
      </div>
      {trailing && (
        <div className="shrink-0 text-xs font-medium text-on-surface-variant">
          {trailing}
        </div>
      )}
    </div>
  );
}
