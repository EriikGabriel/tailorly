/** Intervalo entre consultas enquanto o servidor ainda processa o Currículo Base. */
export const EXTRACTION_POLL_MS = 1500;

/** Depois de concluída sem fatos, por quanto tempo ainda vale perguntar (a gravação pode atrasar). */
const EMPTY_RESULT_GRACE_MS = 60_000;

const RUNNING_STATUSES = ["QUEUED", "PROCESSING"];

type RunLike = { status: string; finishedAt?: string | null };

export function isExtractionRunning(status: string | undefined): boolean {
  return status !== undefined && RUNNING_STATUSES.includes(status);
}

/** Polling da própria execução: só enquanto ela está na fila ou processando. */
export function extractionRunInterval(
  latestRun: RunLike | undefined,
): number | false {
  return isExtractionRunning(latestRun?.status) ? EXTRACTION_POLL_MS : false;
}

/**
 * Polling dos fatos e pendências. Enquanto a execução roda não há o que buscar: os fatos só são
 * gravados ao concluir, e a transição para "concluída" dispara uma única consulta (ver
 * {@link extractionJustCompleted}). Só resta o caso de uma execução recém-concluída cujo resultado
 * ainda veio vazio, que é reconsultado por um curto período.
 */
export function interpretationRefetchInterval(input: {
  latestRun: RunLike | undefined;
  loadedCount: number | undefined;
  now?: number;
}): number | false {
  const { latestRun, loadedCount, now = Date.now() } = input;
  if (
    latestRun?.status === "COMPLETED" &&
    loadedCount === 0 &&
    latestRun.finishedAt &&
    now - Date.parse(latestRun.finishedAt) < EMPTY_RESULT_GRACE_MS
  ) {
    return EXTRACTION_POLL_MS;
  }
  return false;
}

/** A execução acabou de sair de "em andamento" para "concluída": hora de buscar os fatos uma vez. */
export function extractionJustCompleted(
  previous: string | undefined,
  current: string | undefined,
): boolean {
  return isExtractionRunning(previous) && current === "COMPLETED";
}
