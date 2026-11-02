import { useBaseDocumentExtraction } from "@queries/base-document";
import { useDueDiligenceHistory } from "@queries/due-diligence";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useEffect, useRef } from "react";
import { notify } from "@/stores/notification-store";
import { extractionFailureMessage } from "@/utils/extraction-failures";

const TERMINAL_RUN = new Set(["COMPLETED", "FAILED"]);

/**
 * Avisa o término de processos que correm no servidor sem uma mutação do navegador esperando por eles:
 * a extração do Currículo Base e a due diligence que a esteira inicia ao ler uma vaga. Só avisa quem
 * foi visto em andamento (ou nasceu depois que o app abriu), para não repetir o que já terminou antes.
 */
export function BackgroundNotifications() {
  useExtractionNotice();
  useDueDiligenceNotice();
  return null;
}

function useExtractionNotice() {
  const versionId = useBaseCvStore((state) => state.baseVersionId) ?? undefined;
  const run = useBaseDocumentExtraction(versionId).data?.[0];
  const seen = useRef(new Map<string, string>());

  useEffect(() => {
    if (!run?.id) return;
    const previous = seen.current.get(run.id);
    seen.current.set(run.id, run.status);
    if (
      !previous ||
      TERMINAL_RUN.has(previous) ||
      !TERMINAL_RUN.has(run.status)
    )
      return;

    if (run.status === "COMPLETED") {
      notify({
        kind: "success",
        title: "Extração do Currículo Base concluída",
        description:
          "Revise os fatos extraídos e aprove para ativar o Ground Truth.",
        href: "/base",
      });
    } else {
      notify({
        kind: "error",
        title: "A extração do Currículo Base falhou",
        description: extractionFailureMessage(run.errorCode),
        href: "/base",
      });
    }
  }, [run?.id, run?.status, run?.errorCode]);
}

function useDueDiligenceNotice() {
  const history = useDueDiligenceHistory().data;
  const mountedAt = useRef(Date.now());
  const seen = useRef(new Map<string, string>());

  useEffect(() => {
    for (const item of history ?? []) {
      // Pedidos manuais já avisam pela própria mutação.
      if (item.triggerKind !== "pipeline") continue;
      const previous = seen.current.get(item.id);
      seen.current.set(item.id, item.status);
      const finished = item.status !== "running";
      const wasRunning = previous === "running";
      const bornAfterOpen =
        previous === undefined &&
        Date.parse(item.createdAt) >= mountedAt.current;
      if (!finished || !(wasRunning || bornAfterOpen)) continue;

      if (item.status === "completed") {
        notify({
          kind: "success",
          title: "Due diligence da empresa pronta",
          description: `${item.companyName} · ${item.factorsWithEvidence}/${item.factorsTotal} fatores com fontes`,
          href: "/due-diligence",
        });
      } else {
        notify({
          kind: "error",
          title: "Due diligence automática não concluída",
          description: `${item.companyName}. Isso não indica nada sobre a empresa; peça de novo na página.`,
          href: "/due-diligence",
        });
      }
    }
  }, [history]);
}
