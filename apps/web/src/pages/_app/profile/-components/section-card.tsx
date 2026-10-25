import type { ComponentType, ReactNode } from "react";

type SectionCardProps = {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  title: string;
  description?: string;
  trailing?: ReactNode;
  children: ReactNode;
};

export function SectionCard({
  icon: Icon,
  title,
  description,
  trailing,
  children,
}: SectionCardProps) {
  return (
    <section className="flex w-full flex-col gap-6 rounded-xl bg-card p-6 shadow-xs">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="flex items-center gap-2 text-headline-sm font-semibold text-primary-950">
            <Icon aria-hidden className="size-5 shrink-0" />
            {title}
          </h2>
          {description && (
            <p className="max-w-2xl text-body-sm text-on-surface-variant">
              {description}
            </p>
          )}
        </div>
        {trailing}
      </div>
      {children}
    </section>
  );
}

export function FieldLabel({
  children,
  trailing,
}: {
  children: ReactNode;
  trailing?: ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-label-md font-medium text-primary-950">
        {children}
      </span>
      {trailing}
    </div>
  );
}

export function TextField({
  label,
  value,
  onChange,
  trailing,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  trailing?: ReactNode;
  type?: "text" | "email" | "tel" | "url";
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <FieldLabel trailing={trailing}>{label}</FieldLabel>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-lg bg-surface-container-low px-4 py-2.5 text-body-md text-primary-950 outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50"
      />
    </div>
  );
}
