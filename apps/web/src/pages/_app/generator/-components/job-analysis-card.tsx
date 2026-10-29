import type {
  Company,
  JobAnalysis,
  JobPostingDetailResponse,
  JobPostingResponse,
  JobPostingVersionResponse,
  Requirement,
} from "@lib/api/generated/model";
import {
  useDeleteJobPosting,
  useJobPostingList,
  useJobPostingVersion,
  useReanalyzeJobPosting,
  useSelectedJobPosting,
} from "@queries/job-posting";
import {
  AlertTriangle,
  Briefcase,
  Building2,
  Clock,
  RefreshCw,
  Tag,
  Trash2,
} from "@react-zero-ui/icon-sprite";
import { useAuthStore } from "@stores/auth-store";
import { useJobPostingStore } from "@stores/job-posting-store";
import { format, isValid, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import type { ReactNode } from "react";

const workModels: Record<string, string> = {
  remote: "Remoto",
  hybrid: "Híbrido",
  onsite: "Presencial",
};

const seniorities: Record<string, string> = {
  intern: "Estágio",
  junior: "Júnior",
  mid: "Pleno",
  senior: "Sênior",
  staff: "Staff / Principal",
  lead: "Liderança",
};

const categories: Record<string, string> = {
  technology: "Tecnologia",
  experience: "Experiência",
  education: "Formação",
  language: "Idioma",
  certification: "Certificação",
  soft_skill: "Comportamental",
  other: "Outro",
};

const warnings: Record<string, string> = {
  expired: "O prazo de inscrição desta vaga já passou.",
  job_not_published: "A plataforma não marca esta vaga como publicada.",
  limited_content:
    "A página tem pouco texto; os dados podem estar incompletos.",
  sections_not_detected:
    "Não foi possível separar requisitos e responsabilidades pelos títulos do texto.",
};

const platforms: Record<string, string> = {
  gupy: "Gupy",
  "json-ld": "Dados estruturados",
  generic: "Página web",
  text: "Texto colado",
};

function formatDate(value: string | undefined, withTime = false) {
  if (!value) return null;
  const date = parseISO(value);
  return isValid(date)
    ? format(date, withTime ? "dd/MM/yyyy HH:mm" : "dd/MM/yyyy", {
        locale: ptBR,
      })
    : null;
}

function Chip({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "neutral" | "primary" | "warning";
}) {
  const tones = {
    neutral: "bg-surface-container text-on-surface-variant",
    primary: "bg-secondary-container text-on-secondary-container",
    warning: "bg-error-container text-on-error-container",
  } as const;
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold tracking-[0.3px] ${tones[tone]}`}
    >
      {children}
    </span>
  );
}

function Section({
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

function BulletList({ items }: { items: readonly string[] }) {
  return (
    <ul className="flex list-disc flex-col gap-1 pl-5 text-[13px] leading-5 text-on-surface">
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

function RequirementList({
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
                <Chip>{categories[item.category] ?? item.category}</Chip>
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

/** Quando a IA não rodou, mostra a divisão por títulos do texto como requisitos sem categoria. */
function fallbackRequirements(details: JobPostingResponse) {
  const toRequirements = (
    items: readonly string[] | undefined,
    importance: string,
  ): Requirement[] =>
    (items ?? []).map((text) => ({ text, importance, evidence: text }));
  return {
    required: toRequirements(details.requirements, "required"),
    preferred: toRequirements(details.niceToHave, "preferred"),
  };
}

function RequirementsBlock({
  details,
  analysis,
}: {
  details: JobPostingResponse;
  analysis: JobAnalysis | undefined;
}) {
  const grouped = analysis
    ? {
        required: (analysis.requirements ?? []).filter(
          (item) => item.importance === "required",
        ),
        preferred: (analysis.requirements ?? []).filter(
          (item) => item.importance === "preferred",
        ),
      }
    : fallbackRequirements(details);

  if (grouped.required.length === 0 && grouped.preferred.length === 0) {
    return (
      <p className="text-[13px] text-on-surface-variant">
        Nenhum requisito foi identificado no texto da vaga.
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-3">
      <RequirementList title="Obrigatórios" items={grouped.required} />
      <RequirementList title="Desejáveis" items={grouped.preferred} />
    </div>
  );
}

function CompanyBlock({ company }: { company: Company }) {
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
              className="rounded-full bg-surface-container px-2.5 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container-high"
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
              className="rounded-full bg-surface-container px-2.5 py-1 text-xs font-semibold capitalize text-secondary-700 hover:bg-surface-container-high"
            >
              {name}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}

function AnalysisStatus({
  version,
  onRetry,
  retrying,
}: {
  version: JobPostingVersionResponse;
  onRetry: () => void;
  retrying: boolean;
}) {
  const status = version.analysisStatus;
  if (status === "completed") return null;

  const messages: Record<string, string> = {
    unavailable:
      "A análise por IA não está habilitada para esta conta; mostrando a divisão do texto por títulos.",
    failed:
      "A análise por IA falhou. A vaga foi salva e você pode tentar de novo.",
    quota_exceeded:
      "O limite de análises por IA foi atingido. Crie uma conta para continuar.",
  };
  return (
    <div className="flex items-start justify-between gap-2 rounded-lg bg-surface-container p-2.5 text-[13px] leading-5 text-on-surface-variant">
      <span>{messages[status ?? ""] ?? "Análise por IA indisponível."}</span>
      {status === "failed" && (
        <button
          type="button"
          onClick={onRetry}
          disabled={retrying}
          className="inline-flex shrink-0 items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container-high disabled:opacity-50"
        >
          <RefreshCw
            className={`size-3 ${retrying ? "animate-spin" : ""}`}
            aria-hidden="true"
          />
          Tentar de novo
        </button>
      )}
    </div>
  );
}

function SavedJobPostings() {
  const list = useJobPostingList();
  const select = useJobPostingStore((state) => state.select);
  const accountToken = useAuthStore((state) => state.accessToken);
  const items = list.data ?? [];
  if (items.length === 0) return null;

  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
        Vagas salvas:
      </p>
      <ul className="flex flex-col gap-1">
        {items.map((item) => (
          <li key={item.id}>
            <button
              type="button"
              onClick={() =>
                item.id &&
                select(item.id, accountToken ? "account" : "anonymous")
              }
              className="flex w-full items-center justify-between gap-2 rounded-lg bg-surface-container px-3 py-2 text-left text-[13px] text-on-surface hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-ring"
            >
              <span className="truncate font-semibold">
                {item.title ?? "Vaga sem título"}
                {item.companyName ? ` · ${item.companyName}` : ""}
              </span>
              <span className="shrink-0 text-xs text-on-surface-variant">
                v{item.latestVersionNumber}
                {item.expired ? " · encerrada" : ""}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

function VersionPicker({ detail }: { detail: JobPostingDetailResponse }) {
  const selected = useJobPostingStore((state) => state.selected);
  const setVersion = useJobPostingStore((state) => state.setVersion);
  const versions = detail.versions ?? [];
  const latest = detail.latest?.versionNumber ?? 1;
  if (versions.length < 2) return null;

  return (
    <label className="flex items-center gap-2 text-xs font-medium text-on-surface-variant">
      Versão
      <select
        value={selected?.versionNumber ?? latest}
        onChange={(event) => {
          const value = Number(event.target.value);
          setVersion(value === latest ? null : value);
        }}
        className="rounded-md bg-surface-container px-2 py-1 text-xs text-on-surface"
      >
        {versions.map((item) => (
          <option key={item.versionNumber} value={item.versionNumber}>
            v{item.versionNumber} · {formatDate(item.createdAt) ?? ""}
            {item.versionNumber === latest ? " (atual)" : ""}
          </option>
        ))}
      </select>
    </label>
  );
}

export function JobAnalysisCard() {
  const selected = useJobPostingStore((state) => state.selected);
  const clear = useJobPostingStore((state) => state.clear);
  const detail = useSelectedJobPosting();
  const { version } = useJobPostingVersion(detail.data);
  const reanalyze = useReanalyzeJobPosting();
  const remove = useDeleteJobPosting();

  if (!selected) return <SavedJobPostings />;

  if (detail.isError) {
    return (
      <div
        role="alert"
        className="flex items-center justify-between gap-2 rounded-lg bg-error-container p-3 text-[13px] text-on-error-container"
      >
        Não foi possível abrir a vaga salva.
        <button
          type="button"
          onClick={clear}
          className="font-semibold underline"
        >
          Fechar
        </button>
      </div>
    );
  }
  if (!detail.data || !version?.details) {
    return (
      <p role="status" className="text-[13px] text-on-surface-variant">
        Carregando vaga...
      </p>
    );
  }

  const details = version.details;
  const analysis = version.analysis;
  const location = [details.location?.city, details.location?.state]
    .filter(Boolean)
    .join(", ");
  const expiresAt = formatDate(details.expiresAt);
  const readAt = formatDate(version.createdAt, true);
  const checkedAt = formatDate(version.lastCheckedAt, true);

  return (
    <article
      aria-label="Análise da vaga"
      className="flex flex-col gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-3.75"
    >
      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h4 className="text-[15px] font-bold leading-5 text-on-surface">
            {details.title ?? "Vaga sem título"}
          </h4>
          <p className="flex items-center gap-1 text-[13px] text-on-surface-variant">
            <Building2 className="size-3.5" aria-hidden="true" />
            {details.company?.name ?? "Empresa não informada"}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            aria-label="Descartar vaga salva"
            title="Descartar vaga salva"
            disabled={remove.isPending}
            onClick={() =>
              detail.data.posting?.id && remove.mutate(detail.data.posting.id)
            }
            className="rounded-md p-1.5 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-ring disabled:opacity-50"
          >
            <Trash2 className="size-4" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div className="flex flex-wrap gap-1.5">
        {details.platform && (
          <Chip>{platforms[details.platform] ?? details.platform}</Chip>
        )}
        {analysis?.seniority && (
          <Chip tone="primary">
            <Briefcase className="size-3" aria-hidden="true" />
            {seniorities[analysis.seniority] ?? analysis.seniority}
          </Chip>
        )}
        {details.workModel && (
          <Chip>{workModels[details.workModel] ?? details.workModel}</Chip>
        )}
        {details.employmentType && <Chip>{details.employmentType}</Chip>}
        {location && <Chip>{location}</Chip>}
        {expiresAt && (
          <Chip tone={details.expired ? "warning" : "neutral"}>
            <Clock className="size-3" aria-hidden="true" />
            {details.expired ? "Encerrada em" : "Até"} {expiresAt}
          </Chip>
        )}
      </div>

      {(details.warnings ?? []).length > 0 && (
        <ul className="flex flex-col gap-1">
          {details.warnings?.map((warning) => (
            <li
              key={warning}
              className="flex items-start gap-1.5 text-[13px] leading-5 text-on-surface-variant"
            >
              <AlertTriangle
                className="mt-0.5 size-3.5 shrink-0"
                aria-hidden="true"
              />
              {warnings[warning] ?? warning}
            </li>
          ))}
        </ul>
      )}

      <AnalysisStatus
        version={version}
        retrying={reanalyze.isPending}
        onRetry={() =>
          version.jobPostingId &&
          version.versionNumber &&
          reanalyze.mutate({
            postingId: version.jobPostingId,
            versionNumber: version.versionNumber,
          })
        }
      />

      <RequirementsBlock details={details} analysis={analysis} />

      {(analysis?.technologies ?? []).length > 0 && (
        <div className="flex flex-col gap-1.5">
          <p className="flex items-center gap-1 text-xs font-semibold uppercase tracking-[0.6px] text-on-surface-variant">
            <Tag className="size-3" aria-hidden="true" />
            Tecnologias citadas
          </p>
          <div className="flex flex-wrap gap-1">
            {analysis?.technologies?.map((technology) => (
              <Chip key={technology} tone="primary">
                {technology}
              </Chip>
            ))}
          </div>
        </div>
      )}

      {(details.responsibilities ?? []).length > 0 && (
        <Section
          title="Responsabilidades"
          count={details.responsibilities?.length}
        >
          <BulletList items={details.responsibilities ?? []} />
        </Section>
      )}
      {(details.benefits ?? []).length > 0 && (
        <Section title="Benefícios" count={details.benefits?.length}>
          <BulletList items={details.benefits ?? []} />
        </Section>
      )}
      {(details.hiringProcess ?? []).length > 0 && (
        <Section
          title="Processo seletivo"
          count={details.hiringProcess?.length}
        >
          <ol className="flex list-decimal flex-col gap-1 pl-5 text-[13px] leading-5 text-on-surface">
            {details.hiringProcess?.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </Section>
      )}
      {details.company && (
        <Section title="Sobre a empresa">
          <CompanyBlock company={details.company} />
        </Section>
      )}

      <footer className="flex flex-col gap-1.5 border-t border-outline-variant/60 pt-2 text-xs text-on-surface-variant">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <VersionPicker detail={detail.data} />
          <span>
            Versão {version.versionNumber}
            {readAt ? ` · lida em ${readAt}` : ""}
            {checkedAt && checkedAt !== readAt
              ? ` · conferida em ${checkedAt}`
              : ""}
          </span>
        </div>
        {details.finalUrl && (
          <a
            href={details.finalUrl}
            target="_blank"
            rel="noreferrer noopener"
            className="truncate text-secondary-700 hover:underline"
          >
            {details.finalUrl}
          </a>
        )}
        <button
          type="button"
          onClick={clear}
          className="self-start font-semibold text-secondary-700 hover:underline"
        >
          Ver vagas salvas
        </button>
      </footer>
    </article>
  );
}
