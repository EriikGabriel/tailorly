const fieldLabels: Record<string, string> = {
  fullName: "Nome completo",
  name: "Nome",
  email: "E-mail",
  phone: "Telefone",
  headline: "Cargo atual",
  title: "Título",
  role: "Cargo",
  position: "Cargo",
  location: "Localização",
  city: "Cidade",
  state: "Estado",
  country: "País",
  summary: "Resumo profissional",
  linkedin: "LinkedIn",
  linkedinUrl: "LinkedIn",
  github: "GitHub",
  githubUrl: "GitHub",
  portfolio: "Portfólio",
  website: "Site",
  personalWebsite: "Site pessoal",
  url: "Link",
  company: "Empresa",
  organization: "Organização",
  employer: "Empresa",
  startDate: "Data de início",
  endDate: "Data de término",
  period: "Período",
  description: "Descrição",
  achievement: "Conquista",
  institution: "Instituição",
  school: "Instituição",
  degree: "Grau / Curso",
  course: "Curso",
  field: "Área de estudo",
  issuer: "Emissor",
  credential: "Credencial",
  language: "Idioma",
  level: "Nível",
  skill: "Habilidade",
  technology: "Tecnologia",
  technologies: "Tecnologias",
  employment: "Vínculo",
  status: "Situação",
  start: "Início",
  end: "Término",
  proficiency: "Proficiência",
  context: "Contexto",
  statement: "Fato / resultado",
  metric: "Métrica",
  link: "Link do projeto",
  source: "Referência no documento",
  issued: "Emissão",
  expires: "Validade",
  whatsapp: "WhatsApp",
};

// Chaves ambíguas entre categorias (ex.: "name") ganham rótulo específico.
const categoryFieldLabels: Record<string, Record<string, string>> = {
  identity: { name: "Nome completo" },
  certifications: { name: "Certificação" },
  technologies: { name: "Tecnologia / competência" },
  projects: { title: "Projeto" },
  experience: { description: "Responsabilidades e contexto" },
  volunteering: {
    role: "Função",
    description: "Atividades realizadas",
  },
};

function humanize(key: string) {
  const words = key
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .trim()
    .toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

// Fatos de listas (experiência, projetos...) usam chaves como "1:company".
export function factFieldLabel(fieldKey: string, category?: string) {
  const match = /^(\d+):(.*)$/.exec(fieldKey);
  const key = match ? match[2] : fieldKey;
  const label =
    (category ? categoryFieldLabels[category]?.[key] : undefined) ??
    fieldLabels[key] ??
    humanize(key);
  return match ? `${label} (item ${Number(match[1]) + 1})` : label;
}

export function formatFactValue(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value.trim();
  if (Array.isArray(value)) return value.map(formatFactValue).join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
