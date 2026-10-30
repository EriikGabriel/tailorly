import type { Requirement } from "@lib/api/generated/model";
import { REQUIREMENT_CATEGORY_LABELS } from "../../-constants/job-labels";
import { Chip } from "./chip";

/** Grupo de requisitos (obrigatórios ou desejáveis) com categoria, anos mínimos e tecnologias. */
export function RequirementList({
  title,
  items,
}: {
  title: string;
  items: readonly Requirement[];
}) {
  if (items.length === 0) return null;
  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-xs font-semibold uppercase tracking-[0.6px] text-on-surface-variant">
        {title} · {items.length}
      </p>
      <ul className="flex flex-col gap-1.5">
        {items.map((item) => (
          <li
            key={`${item.text}-${item.evidence}`}
            title={
              item.evidence ? `Trecho da vaga: ${item.evidence}` : undefined
            }
            className="flex flex-col gap-1 rounded-lg bg-surface-container-low p-2.5"
          >
            <span className="text-[13px] leading-5 text-on-surface">
              {item.text}
            </span>
            <span className="flex flex-wrap items-center gap-1">
              {item.category && (
                <Chip>
                  {REQUIREMENT_CATEGORY_LABELS[item.category] ?? item.category}
                </Chip>
              )}
              {item.minYears ? <Chip>{item.minYears}+ anos</Chip> : null}
              {item.technologies?.map((technology) => (
                <Chip key={technology} tone="primary">
                  {technology}
                </Chip>
              ))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
