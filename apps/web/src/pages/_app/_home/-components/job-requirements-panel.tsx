import { Check, IconExclamationMark, Target } from "@react-zero-ui/icon-sprite";
import type {
  RequirementMatch,
  RequirementStatus,
} from "@/utils/job-fact-match";

const statusLabels: Record<RequirementStatus, string> = {
  supported: "No seu currículo",
  unconfirmed: "No seu currículo · confirme abaixo",
  missing: "Não encontrado · não será inventado",
};

const statusTones: Record<RequirementStatus, string> = {
  supported: "bg-secondary-container text-on-secondary-container",
  unconfirmed: "bg-surface-container text-on-surface",
  missing: "bg-error-container text-on-error-container",
};

const MAX_REQUIRED = 6;
const MAX_PREFERRED = 4;

function Row({ item }: { item: RequirementMatch }) {
  const names = item.terms.join(", ");
  return (
    <li className="flex items-center justify-between gap-3 py-1.5">
      <span className="min-w-0 text-sm text-on-surface">
        <span className="font-semibold">{names}</span>
        {item.requirement.minYears ? (
          <span className="ml-1.5 text-xs text-on-surface-variant">
            {item.requirement.minYears}+ anos
          </span>
        ) : null}
      </span>
      <span
        className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${statusTones[item.status]}`}
      >
        {item.status === "supported" ? (
          <Check className="size-3" aria-hidden="true" />
        ) : (
          <IconExclamationMark className="size-3" aria-hidden="true" />
        )}
        {statusLabels[item.status]}
      </span>
    </li>
  );
}

/** O que a vaga mais pede e onde isso aparece nos fatos do Currículo Base. */
export function JobRequirementsPanel({
  title,
  company,
  matches,
}: {
  title?: string;
  company?: string;
  matches: readonly RequirementMatch[];
}) {
  const required = matches.filter((item) => item.importance === "required");
  const preferred = matches.filter((item) => item.importance === "preferred");
  const shownRequired = required.slice(0, MAX_REQUIRED);
  const shownPreferred = preferred.slice(0, MAX_PREFERRED);
  const hidden =
    required.length -
    shownRequired.length +
    (preferred.length - shownPreferred.length);

  return (
    <section
      aria-label="O que a vaga mais pede"
      className="rounded-lg border border-outline-variant/40 bg-surface-container-low p-4"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center text-sm font-semibold">
          <Target className="mr-2 size-4" aria-hidden="true" />O que a vaga mais
          pede
        </h3>
        {(title || company) && (
          <span className="max-w-full truncate text-xs text-on-surface-variant">
            {[title, company].filter(Boolean).join(" · ")}
          </span>
        )}
      </div>

      {shownRequired.length > 0 && (
        <div className="mt-2">
          <p className="text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">
            Obrigatórios
          </p>
          <ul className="divide-y divide-outline-variant/30">
            {shownRequired.map((item) => (
              <Row key={item.terms.join("|")} item={item} />
            ))}
          </ul>
        </div>
      )}
      {shownPreferred.length > 0 && (
        <div className="mt-2">
          <p className="text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">
            Desejáveis
          </p>
          <ul className="divide-y divide-outline-variant/30">
            {shownPreferred.map((item) => (
              <Row key={item.terms.join("|")} item={item} />
            ))}
          </ul>
        </div>
      )}
      {hidden > 0 && (
        <p className="mt-2 text-xs text-on-surface-variant">
          + {hidden} {hidden === 1 ? "requisito" : "requisitos"} de menor peso.
        </p>
      )}
    </section>
  );
}
