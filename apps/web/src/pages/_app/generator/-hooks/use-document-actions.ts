import { apiErrorMessage } from "@api/error-message";
import type { CvGenerationResponse } from "@api/generated/model";
import { downloadCvFile, useSaveCvTemplate } from "@queries/cv-generation";
import { useState } from "react";

/** Ações sobre o currículo gerado: salvar o template editado (sem gerar de novo) e baixar os arquivos. */
export function useDocumentActions(
  generation: CvGenerationResponse | undefined,
  token: string | null,
  templateDraft: string | null,
  onSaved: (content: string) => void,
) {
  const save = useSaveCvTemplate();
  const [error, setError] = useState<string | null>(null);
  const generationId = generation?.id ?? null;

  async function saveTemplate() {
    if (!generation?.ownerId || !token || templateDraft === null) return;
    setError(null);
    try {
      await save.mutateAsync({
        ownerId: generation.ownerId,
        token,
        content: templateDraft,
      });
      onSaved(templateDraft);
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
    saveTemplate,
    download,
    saving: save.isPending,
    error,
  };
}
