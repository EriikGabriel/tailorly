import type { DraftFields } from "@@types/review-model";
import {
  durationLabel,
  formatPartialDate,
  formatPeriod,
  isCurrentStatus,
} from "@/lib/review-format";

export type BadgeTone = "success" | "info" | "neutral";

export interface RecordSummary {
  title: string;
  subtitle?: string;
  meta?: string;
  badges: { label: string; tone: BadgeTone }[];
}

const clean = (value: string | undefined) => (value ?? "").trim();

function compact<T>(items: (T | null | false | undefined | "")[]): T[] {
  return items.filter((item): item is T => Boolean(item));
}

export function summarizeExperience(fields: DraftFields): RecordSummary {
  const role = clean(fields.role);
  const company = clean(fields.company);
  const current = isCurrentStatus(fields.status);
  const period = formatPeriod(fields.start, fields.end, current);
  const duration = durationLabel(fields.start, fields.end, current);
  const employment = clean(fields.employment);
  const status = clean(fields.status);

  return {
    title: role || company || "Nova experiência",
    subtitle: role && company ? company : undefined,
    meta: compact([period, duration && `(${duration})`]).join(" "),
    badges: compact([
      employment && { label: employment, tone: "neutral" as const },
      status && {
        label: current ? "Atual" : status,
        tone: current ? ("success" as const) : ("neutral" as const),
      },
    ]),
  };
}

export function summarizeDegree(fields: DraftFields): RecordSummary {
  const current = isCurrentStatus(fields.status);
  const status = clean(fields.status);
  return {
    title: clean(fields.degree) || "Nova formação",
    subtitle: clean(fields.institution) || undefined,
    meta: formatPeriod(fields.start, fields.end, current, "em andamento"),
    badges: compact([
      status && {
        label: status,
        tone: current ? ("info" as const) : ("success" as const),
      },
    ]),
  };
}

export function summarizeCertificate(fields: DraftFields): RecordSummary {
  const issued = formatPartialDate(fields.issued);
  const expires = formatPartialDate(fields.expires);
  return {
    title: clean(fields.name) || "Nova certificação",
    subtitle: clean(fields.issuer) || undefined,
    meta: compact([
      issued && `Emitida em ${issued}`,
      expires && `válida até ${expires}`,
    ]).join(" · "),
    badges: [],
  };
}

export function summarizeProject(fields: DraftFields): RecordSummary {
  const metric = clean(fields.metric);
  return {
    title: clean(fields.title) || "Nova realização",
    subtitle: clean(fields.context) || undefined,
    badges: compact([metric && { label: metric, tone: "info" as const }]),
  };
}

export function summarizeVolunteering(fields: DraftFields): RecordSummary {
  const role = clean(fields.role);
  const organization = clean(fields.organization);
  const current = isCurrentStatus(fields.status);
  const period = formatPeriod(fields.start, fields.end, current);
  const duration = durationLabel(fields.start, fields.end, current);
  const status = clean(fields.status);

  return {
    title: role || organization || "Nova atividade voluntária",
    subtitle: role && organization ? organization : undefined,
    meta: compact([period, duration && `(${duration})`]).join(" "),
    badges: compact([
      status && {
        label: current ? "Atual" : status,
        tone: current ? ("success" as const) : ("neutral" as const),
      },
    ]),
  };
}
