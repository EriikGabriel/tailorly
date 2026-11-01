import { useGenerationProgress } from "@queries/generation-progress";
import { AiStageLabel, type OrbState } from "./ai-stage-label";

/**
 * `analyzing`: lendo a vaga; `collecting`: buscando evidências complementares; `generating`: a
 * esteira de geração no servidor.
 */
export type GenerationPhase = "analyzing" | "collecting" | "generating";

const ANALYZING = {
  label: "Lendo e analisando a vaga",
  orb: "searching",
} as const;

const COLLECTING = {
  label: "Buscando evidências complementares",
  orb: "searching",
} as const;

const STAGES: Record<string, { label: string; orb: OrbState }> = {
  PREPARING: { label: "Montando com os fatos aprovados", orb: "connecting" },
  REWRITING: { label: "A IA reescreve o texto para a vaga", orb: "composing" },
  RENDERING: { label: "Compilando o PDF", orb: "shaping" },
  SCORING: { label: "Avaliando a leitura por ATS", orb: "solving" },
  REFINING: { label: "A IA ajusta o layout", orb: "weaving" },
  SAVING: { label: "Salvando os arquivos", orb: "breathing" },
};

/** Etapa atual da leitura da vaga ou da geração, para usar dentro do próprio botão. */
export function GenerationStageLabel({
  phase,
  theme,
}: {
  phase: GenerationPhase;
  theme?: "dark" | "light";
}) {
  const generating = phase === "generating";
  const progress = useGenerationProgress(generating);
  // Um resultado já concluído é de uma geração anterior; só vale o que está em andamento.
  const snapshot = progress.data?.status === "RUNNING" ? progress.data : null;

  if (!generating) {
    const idle = phase === "collecting" ? COLLECTING : ANALYZING;
    return <AiStageLabel orb={idle.orb} label={idle.label} theme={theme} />;
  }
  const stage = STAGES[snapshot?.stage ?? "PREPARING"] ?? STAGES.PREPARING;
  const label =
    snapshot?.stage === "REFINING" && snapshot.round > 0
      ? `${stage.label} (rodada ${snapshot.round})`
      : stage.label;
  return (
    <AiStageLabel
      orb={stage.orb}
      label={label}
      theme={theme}
      title={snapshot?.detail ?? undefined}
    />
  );
}
