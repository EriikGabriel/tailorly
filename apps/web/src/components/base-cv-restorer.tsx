import {
  baseDocumentFile,
  baseVersionFindByOwner,
} from "@lib/api/base-restore";
import { baseDocumentFindById } from "@lib/api/generated/tailorly";
import { useViewer } from "@queries/cv-generation";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useEffect, useRef } from "react";

/** Estados em que a revisão ainda pode continuar (espelha `BaseVersionStatus.REVIEWABLE` no servidor). */
const REVIEWABLE = new Set(["CANDIDATE", "EXTRACTED", "ACTIVE"]);

/**
 * Depois de recarregar a página o estado em memória some, mas o Currículo Base continua no servidor.
 * Com uma conta (ou sessão de visitante) válida e nada selecionado, reabre a versão mais recente em
 * revisão: os fatos, as pendências e as correções vêm do servidor pelas consultas normais; aqui só se
 * recupera o PDF original para o visualizador, sem novo envio nem OCR.
 */
export function BaseCvRestorer() {
  const { ownerId, token, kind } = useViewer();
  const attempted = useRef<string | null>(null);

  useEffect(() => {
    if (!ownerId || !token || !kind) {
      attempted.current = null;
      return;
    }
    const key = `${kind}:${ownerId}`;
    if (attempted.current === key) return;
    attempted.current = key;

    const current = useBaseCvStore.getState();
    if (current.file || current.baseVersionId) return;

    let cancelled = false;
    (async () => {
      try {
        const versions = await baseVersionFindByOwner(ownerId, {
          accessToken: token,
        });
        const version = versions.find((item) => REVIEWABLE.has(item.status));
        if (!version || cancelled) return;
        const document = await baseDocumentFindById(version.documentId, {
          accessToken: token,
        });
        const blob = await baseDocumentFile(document.id, {
          accessToken: token,
        });
        if (cancelled) return;
        const file = new File([blob], document.originalFilename, {
          type: "application/pdf",
        });
        useBaseCvStore.getState().restore(file, version.id, kind);
      } catch {
        // Sem restauração a página continua como um envio novo; nada é perdido no servidor.
      }
    })();

    return () => {
      cancelled = true;
      // Um efeito refeito (StrictMode) tenta de novo em vez de ficar sem restauração.
      attempted.current = null;
    };
  }, [ownerId, token, kind]);

  return null;
}
