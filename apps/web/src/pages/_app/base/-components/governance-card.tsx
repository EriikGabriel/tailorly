import type { ReactNode } from "react";

export function GovernanceCard({
  label,
  icon,
  accent = false,
  children,
}: {
  label: string;
  icon: ReactNode;
  accent?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="flex min-w-0 items-center gap-4 rounded-xl bg-card p-4 shadow-md">
      <span
        className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${accent ? "bg-secondary-container text-on-secondary-container" : "bg-surface-container-highest text-primary-950"}`}
      >
        {icon}
      </span>
      <div className="min-w-0 flex-1">
        <h2 className="text-xs font-semibold leading-4 tracking-[0.6px] text-on-surface-variant uppercase">
          {label}
        </h2>
        {children}
      </div>
    </div>
  );
}
