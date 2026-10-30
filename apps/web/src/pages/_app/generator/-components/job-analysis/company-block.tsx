import type { Company } from "@lib/api/generated/model";

const LINK_CLASS =
  "rounded-full bg-surface-container px-2.5 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container-high";

/** Descrição da empresa e links oficiais (site, LinkedIn, Glassdoor etc.). */
export function CompanyBlock({ company }: { company: Company }) {
  const links = Object.entries(company.links ?? {});
  const website = company.website;
  return (
    <div className="flex flex-col gap-2 text-[13px] leading-5 text-on-surface">
      {company.description && (
        <p className="line-clamp-6 whitespace-pre-line">
          {company.description}
        </p>
      )}
      {(website || links.length > 0) && (
        <div className="flex flex-wrap gap-1.5">
          {website && (
            <a
              href={website}
              target="_blank"
              rel="noreferrer noopener"
              className={LINK_CLASS}
            >
              Site oficial
            </a>
          )}
          {links.map(([name, url]) => (
            <a
              key={name}
              href={url}
              target="_blank"
              rel="noreferrer noopener"
              className={`${LINK_CLASS} capitalize`}
            >
              {name}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
