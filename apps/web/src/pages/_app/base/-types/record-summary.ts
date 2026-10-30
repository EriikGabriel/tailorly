export type BadgeTone = "success" | "info" | "neutral";

/** Resumo de uma entrada repetível (cargo, formação etc.) mostrado no cartão recolhido. */
export interface RecordSummary {
  title: string;
  subtitle?: string;
  meta?: string;
  badges: { label: string; tone: BadgeTone }[];
}
