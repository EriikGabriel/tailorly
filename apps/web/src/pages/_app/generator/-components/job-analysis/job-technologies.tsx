import { Tag } from "@react-zero-ui/icon-sprite";
import { Chip } from "./chip";

/** Tecnologias citadas na vaga, identificadas pela análise por IA. */
export function JobTechnologies({
  technologies,
}: {
  technologies: readonly string[];
}) {
  if (technologies.length === 0) return null;
  return (
    <div className="flex flex-col gap-1.5">
      <p className="flex items-center gap-1 text-xs font-semibold uppercase tracking-[0.6px] text-on-surface-variant">
        <Tag className="size-3" aria-hidden="true" />
        Tecnologias citadas
      </p>
      <div className="flex flex-wrap gap-1">
        {technologies.map((technology) => (
          <Chip key={technology} tone="primary">
            {technology}
          </Chip>
        ))}
      </div>
    </div>
  );
}
