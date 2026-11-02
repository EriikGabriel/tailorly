import { apiRequest } from "@api/client";
import { skipToken, useQuery } from "@tanstack/react-query";
import { useViewer } from "./cv-generation";

/** Etapa da geração em curso, como a API informa (`GET /v1/cv/generate/progress/{ownerId}`). */
export type GenerationProgressSnapshot = {
  status: "RUNNING" | "DONE" | "FAILED";
  stage: string;
  detail: string | null;
  round: number;
  startedAt: string;
};

/**
 * Consulta a etapa da geração enquanto `enabled`. A API devolve `204` sem geração em curso, e um
 * resultado de uma geração anterior (concluída há pouco) não deve valer para a atual: quem usa o
 * resultado só considera o que estiver em andamento.
 */
export function useGenerationProgress(enabled: boolean) {
  const { ownerId, token } = useViewer();

  return useQuery<GenerationProgressSnapshot | null>({
    queryKey: ["cv", "generation-progress", ownerId ?? ""],
    queryFn:
      enabled && ownerId && token
        ? async () =>
            (await apiRequest<GenerationProgressSnapshot | undefined>(
              `/v1/cv/generate/progress/${encodeURIComponent(ownerId)}`,
              { accessToken: token },
            )) ?? null
        : skipToken,
    refetchInterval: enabled ? 1200 : false,
    gcTime: 0,
    retry: false,
  });
}
