import type { ReactNode } from "react";

/** Seção recolhível (`<details>`) com título e contador opcional de itens. */
export function CollapsibleSection({
  title,
  count,
  open = false,
  children,
}: {
  title: string;
  count?: number;
  open?: boolean;
  children: ReactNode;
}) {
  return (
    <details
      open={open}
      className="group border-t border-outline-variant/60 pt-2"
    >
      <summary className="flex cursor-pointer list-none items-center justify-between py-1 text-sm font-semibold text-on-surface focus-visible:outline-2 focus-visible:outline-ring">
        <span>{title}</span>
        {count !== undefined && (
          <span className="text-xs font-medium text-on-surface-variant">
            {count}
          </span>
        )}
      </summary>
      <div className="pb-2 pt-1">{children}</div>
    </details>
  );
}
