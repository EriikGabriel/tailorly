// Formatação dos fatos do Currículo Base para exibição. O valor guardado continua o texto
// do fato (ex.: "2022-01", "2015", "(11) 98765-4321"); aqui só se decide como mostrá-lo.

export interface PartialDate {
  year: number;
  month: number | null;
}

const MONTH_PATTERN = /^(\d{4})-(0[1-9]|1[0-2])$/;
const YEAR_PATTERN = /^(\d{4})$/;

/** Aceita "YYYY-MM" (mês e ano) ou "YYYY" (só o ano); qualquer outro formato é inválido. */
export function parsePartialDate(
  value: string | undefined,
): PartialDate | null {
  const text = (value ?? "").trim();
  const month = MONTH_PATTERN.exec(text);
  if (month) return { year: Number(month[1]), month: Number(month[2]) };
  const year = YEAR_PATTERN.exec(text);
  if (year) return { year: Number(year[1]), month: null };
  return null;
}

export function toPartialValue(year: number, month: number | null): string {
  const y = String(year).padStart(4, "0");
  return month === null ? y : `${y}-${String(month).padStart(2, "0")}`;
}

const monthFormatter = new Intl.DateTimeFormat("pt-BR", {
  month: "short",
  timeZone: "UTC",
});
const longMonthFormatter = new Intl.DateTimeFormat("pt-BR", {
  month: "long",
  timeZone: "UTC",
});

export function monthShortName(month: number): string {
  return monthFormatter
    .format(new Date(Date.UTC(2000, month - 1, 1)))
    .replace(".", "");
}

export function monthLongName(month: number): string {
  return longMonthFormatter.format(new Date(Date.UTC(2000, month - 1, 1)));
}

/** "2022-01" -> "jan 2022"; "2015" -> "2015"; texto fora do padrão é devolvido como está. */
export function formatPartialDate(value: string | undefined): string {
  const date = parsePartialDate(value);
  if (!date) return (value ?? "").trim();
  return date.month === null
    ? String(date.year)
    : `${monthShortName(date.month)} ${date.year}`;
}

const CURRENT_STATUS = /^(atual|presente|current|present|em andamento)/i;

export function isCurrentStatus(status: string | undefined): boolean {
  return CURRENT_STATUS.test((status ?? "").trim());
}

/** "jan 2022 — atual", "2015 — 2019", "mar 2019 — dez 2021"; vazio quando não há datas. */
export function formatPeriod(
  start: string | undefined,
  end: string | undefined,
  current: boolean,
  currentLabel = "atual",
): string {
  const from = formatPartialDate(start);
  const to = current ? currentLabel : formatPartialDate(end);
  if (!from && !to) return "";
  if (!from) return to;
  if (!to) return from;
  return `${from} — ${to}`;
}

function plural(count: number, one: string, many: string) {
  return `${count} ${count === 1 ? one : many}`;
}

/** Duração entre duas datas; sem mês em alguma ponta, conta só anos. Nulo se inválida. */
export function durationLabel(
  start: string | undefined,
  end: string | undefined,
  current: boolean,
  now: Date = new Date(),
): string | null {
  const from = parsePartialDate(start);
  if (!from) return null;
  const to = current
    ? { year: now.getUTCFullYear(), month: now.getUTCMonth() + 1 }
    : parsePartialDate(end);
  if (!to) return null;

  if (from.month === null || to.month === null) {
    const years = to.year - from.year;
    return years > 0 ? plural(years, "ano", "anos") : null;
  }

  // Inclusivo: jan/2022 a jan/2022 já conta como 1 mês de vínculo.
  const months = (to.year - from.year) * 12 + (to.month - from.month) + 1;
  if (months < 1) return null;
  const years = Math.floor(months / 12);
  const rest = months % 12;
  if (years === 0) return plural(rest, "mês", "meses");
  if (rest === 0) return plural(years, "ano", "anos");
  return `${plural(years, "ano", "anos")} e ${plural(rest, "mês", "meses")}`;
}

/** Máscara progressiva de telefone brasileiro; números internacionais ficam como digitados. */
export function formatPhone(value: string): string {
  const text = value.trim();
  if (text.startsWith("+") && !text.startsWith("+55")) return text;

  let digits = text.replace(/\D/g, "");
  let countryCode = false;
  if (
    text.startsWith("+55") ||
    (digits.startsWith("55") && digits.length >= 12)
  ) {
    digits = digits.slice(2);
    countryCode = true;
  }
  digits = digits.slice(0, 11);
  if (digits.length === 0) return countryCode ? "+55" : "";

  let masked: string;
  if (digits.length <= 2) masked = `(${digits}`;
  else if (digits.length <= 6)
    masked = `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  else if (digits.length <= 10)
    masked = `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  else
    masked = `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
  return countryCode ? `+55 ${masked}` : masked;
}

export type LinkKind = "github" | "linkedin" | "other";

export interface LinkInfo {
  href: string;
  display: string;
  kind: LinkKind;
}

/** Interpreta um link como foi escrito no documento ("github.com/pessoa/repo"). */
export function describeLink(value: string | undefined): LinkInfo | null {
  const text = (value ?? "").trim();
  if (!text || /\s/.test(text)) return null;
  const href = /^[a-z][a-z0-9+.-]*:\/\//i.test(text) ? text : `https://${text}`;
  let url: URL;
  try {
    url = new URL(href);
  } catch {
    return null;
  }
  if (!url.hostname.includes(".") || !/^https?:$/.test(url.protocol))
    return null;

  const host = url.hostname.replace(/^www\./, "");
  const path = url.pathname.replace(/\/+$/, "");
  const kind: LinkKind = host.endsWith("github.com")
    ? "github"
    : host.endsWith("linkedin.com")
      ? "linkedin"
      : "other";
  return { href, display: `${host}${path}`, kind };
}

export function initials(name: string | undefined): string {
  const words = (name ?? "")
    .trim()
    .split(/\s+/)
    .filter((word) => word.length > 0 && /\p{L}/u.test(word[0]));
  if (words.length === 0) return "";
  const first = words[0][0];
  const last = words.length > 1 ? words[words.length - 1][0] : "";
  return `${first}${last}`.toUpperCase();
}

export const PROFICIENCY_LEVELS = [
  { value: "Básico", label: "Básico" },
  { value: "Intermediário", label: "Intermediário" },
  { value: "Avançado", label: "Avançado" },
  { value: "Fluente", label: "Fluente" },
  { value: "Nativo", label: "Nativo" },
] as const;

const PROFICIENCY_ALIASES: Record<string, number> = {
  basico: 0,
  basic: 0,
  iniciante: 0,
  elementar: 0,
  intermediario: 1,
  intermediate: 1,
  avancado: 2,
  advanced: 2,
  fluente: 3,
  fluent: 3,
  nativo: 4,
  native: 4,
  bilingue: 4,
};

function fold(text: string): string {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").trim().toLowerCase();
}

/** Índice (0-4) do nível de proficiência, ou -1 se o texto não for um nível conhecido. */
export function proficiencyIndex(value: string | undefined): number {
  return PROFICIENCY_ALIASES[fold(value ?? "")] ?? -1;
}
