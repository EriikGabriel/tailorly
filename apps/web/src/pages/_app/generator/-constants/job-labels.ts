/** Rótulos em português para os códigos que a API devolve na análise da vaga. */

export const WORK_MODEL_LABELS: Record<string, string> = {
  remote: "Remoto",
  hybrid: "Híbrido",
  onsite: "Presencial",
};

export const SENIORITY_LABELS: Record<string, string> = {
  intern: "Estágio",
  junior: "Júnior",
  mid: "Pleno",
  senior: "Sênior",
  staff: "Staff / Principal",
  lead: "Liderança",
};

export const REQUIREMENT_CATEGORY_LABELS: Record<string, string> = {
  technology: "Tecnologia",
  experience: "Experiência",
  education: "Formação",
  language: "Idioma",
  certification: "Certificação",
  soft_skill: "Comportamental",
  other: "Outro",
};

export const JOB_WARNING_LABELS: Record<string, string> = {
  expired: "O prazo de inscrição desta vaga já passou.",
  job_not_published: "A plataforma não marca esta vaga como publicada.",
  limited_content:
    "A página tem pouco texto; os dados podem estar incompletos.",
  sections_not_detected:
    "Não foi possível separar requisitos e responsabilidades pelos títulos do texto.",
};

export const JOB_PLATFORM_LABELS: Record<string, string> = {
  gupy: "Gupy",
  "json-ld": "Dados estruturados",
  generic: "Página web",
  text: "Texto colado",
};

/** Mensagem exibida quando a análise por IA não concluiu, por status da versão. */
export const ANALYSIS_STATUS_MESSAGES: Record<string, string> = {
  unavailable:
    "A análise por IA não está habilitada para esta conta; mostrando a divisão do texto por títulos.",
  failed:
    "A análise por IA falhou. A vaga foi salva e você pode tentar de novo.",
  quota_exceeded:
    "O limite de análises por IA foi atingido. Crie uma conta para continuar.",
};
