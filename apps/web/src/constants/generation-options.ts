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

/**
 * Idioma do currículo final. `auto` (padrão, inclusive sem conta) usa o idioma em que a vaga foi
 * escrita; as outras duas opções fixam o idioma. Estes são os únicos valores que o servidor aceita.
 */
export const languageOptions = [
  { value: "auto", label: "Detectar automaticamente da vaga (PT-BR / EN)" },
  { value: "pt-BR", label: "Sempre em Português (Brasil)" },
  { value: "en", label: "Sempre em Inglês" },
] as const;

export type CvLanguagePreference = (typeof languageOptions)[number]["value"];

export const DEFAULT_LANGUAGE: CvLanguagePreference = "auto";

/** Valor vindo do servidor ou de uma geração antiga; qualquer coisa fora das opções vira `auto`. */
export function toLanguagePreference(value: unknown): CvLanguagePreference {
  return languageOptions.some((option) => option.value === value)
    ? (value as CvLanguagePreference)
    : DEFAULT_LANGUAGE;
}
