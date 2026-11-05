import type { CvGenerationResponse } from "@api/generated/model";
import { useBundledCvTemplate } from "@queries/cv-generation";
import { useTemplateDraftStore } from "@stores/template-draft-store";
import { useMemo } from "react";
import { formatTypstTemplate } from "../-utils/format-typst";

/**
 * Código Typst que a pessoa edita. Começa como o template usado na geração (já formatado); o
 * rascunho vive numa store (e em `sessionStorage`) para sobreviver à troca de aba e ao reload.
 * `dirty` indica alterações ainda não salvas; `saved` indica template salvo que a geração aberta
 * ainda não usa (vale na próxima geração); `restore` volta ao template padrão do sistema.
 */
export function useTypstTemplateDraft(
  generation: CvGenerationResponse | undefined,
  token: string | null,
) {
  const bundled = useBundledCvTemplate(generation ? token : null);

  const rawBaseline = generation?.template?.content ?? null;
  const formattedBaseline = useMemo(
    () => (rawBaseline !== null ? formatTypstTemplate(rawBaseline) : null),
    [rawBaseline],
  );

  const generationId = generation?.id ?? null;
  const stored = useTemplateDraftStore((state) => state);
  const own = generationId !== null && stored.generationId === generationId;
  const draft = (own ? stored.content : null) ?? formattedBaseline;
  const savedContent = own ? stored.savedContent : null;
  // O que já está gravado no servidor: o último salvo ou, sem isso, o template da própria geração.
  const persisted = savedContent ?? formattedBaseline;

  function setDraft(value: string | null) {
    if (generationId === null) return;
    stored.setDraft(generationId, value === formattedBaseline ? null : value);
  }

  const template = generation?.template;
  return {
    draft,
    setDraft,
    /** "Nome vN" do template usado nesta geração. */
    templateLabel: template
      ? `Template ${template.name} v${template.version}`
      : "",
    dirty: draft !== null && draft !== persisted,
    /** Há um template salvo diferente do que a geração aberta usa. */
    saved: savedContent !== null && savedContent !== formattedBaseline,
    markSaved(content: string) {
      if (generationId !== null) stored.markSaved(generationId, content);
    },
    canRestore: Boolean(bundled.data),
    restore() {
      if (!bundled.data) return;
      setDraft(
        bundled.data.content ? formatTypstTemplate(bundled.data.content) : null,
      );
    },
  };
}
