import { apiErrorMessage } from "@lib/api/error-message";
import type { CvGenerationResponse } from "@lib/api/generated/model";
import {
  downloadCvFile,
  useGenerateCv,
  useSaveCvTemplate,
} from "@queries/cv-generation";
import { useState } from "react";

/** Ações sobre o currículo gerado: salvar o template editado, gerar de novo e baixar os arquivos. */
export function useDocumentActions(
  generation: CvGenerationResponse | undefined,
  token: string | null,
  templateDraft: string | null,
) {
  const save = useSaveCvTemplate();
  const regenerate = useGenerateCv();
  const [error, setError] = useState<string | null>(null);
  const generationId = generation?.id ?? null;

  async function saveAndRegenerate() {
    if (!generation?.ownerId || !token || templateDraft === null) return;
    setError(null);
    try {
      await save.mutateAsync({
        ownerId: generation.ownerId,
        token,
        content: templateDraft,
      });
      await regenerate.mutateAsync();
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível salvar o template."));
    }
  }

  async function download(format: "pdf" | "markdown") {
    if (!generationId || !token) return;
    setError(null);
    try {
      await downloadCvFile(generationId, format, token);
    } catch (cause) {
      setError(apiErrorMessage(cause, "Não foi possível baixar o arquivo."));
    }
  }

  return {
    saveAndRegenerate,
    download,
    regenerating: regenerate.isPending,
    busy: save.isPending || regenerate.isPending,
    error,
  };
}
