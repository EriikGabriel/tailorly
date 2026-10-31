/** Layouts de currículo embarcados no servidor (ver services/api cv-templates). Só mudam a apresentação. */
export const templateOptions = [
  {
    value: "classic",
    label: "Clássico",
    description: "Uma coluna, sóbrio, o mais simples para triagem.",
  },
  {
    value: "modern",
    label: "Moderno",
    description: "Uma coluna com títulos de seção em cor de destaque.",
  },
  {
    value: "editorial",
    label: "Editorial",
    description: "Serifado, títulos em itálico com filete fino.",
  },
  {
    value: "harvard",
    label: "Harvard",
    description:
      "Formato Harvard: nome centralizado, seções em versalete, datas à direita.",
  },
] as const;

export type TemplateName = (typeof templateOptions)[number]["value"];

export const languageOptions = [
  { value: "auto", label: "Detectar automaticamente da vaga (PT-BR / EN)" },
  { value: "pt-br", label: "Português (Brasil)" },
  { value: "en", label: "Inglês (EN-US)" },
] as const;
