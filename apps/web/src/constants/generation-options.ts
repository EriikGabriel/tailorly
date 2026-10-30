/** Opções de tipografia e idioma oferecidas nos padrões de geração (ainda não persistidas). */
export const typographyOptions = [
  {
    value: "harvard-precision",
    label: "Harvard Precision Tech (A4 Executivo Editorial)",
  },
  { value: "ats-classic", label: "ATS Classic Sans (Letter Compacto)" },
  { value: "editorial-serif", label: "Editorial Serif (A4 Narrativo)" },
] as const;

export const languageOptions = [
  { value: "auto", label: "Detectar automaticamente da vaga (PT-BR / EN)" },
  { value: "pt-br", label: "Português (Brasil)" },
  { value: "en", label: "Inglês (EN-US)" },
] as const;
