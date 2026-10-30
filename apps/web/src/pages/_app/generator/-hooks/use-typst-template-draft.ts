import type { CvGenerationResponse } from "@lib/api/generated/model";
import { useBundledCvTemplate } from "@queries/cv-generation";
import { useEffect, useMemo, useState } from "react";
import { formatTypstTemplate } from "../-utils/format-typst";

/**
 * Código Typst que a pessoa edita. Começa como o template usado na geração (já formatado, para
 * facilitar ver e editar cada variável); `dirty` indica alterações ainda não salvas e `restore`
 * volta ao template padrão do sistema.
 */
export function useTypstTemplateDraft(
  generation: CvGenerationResponse | undefined,
  token: string | null,
) {
  // O template em uso (nome, versão e código) já vem na própria geração; não há o que buscar.
  // O template padrão só é consultado quando há uma geração aberta, para o botão "Restaurar".
  const bundled = useBundledCvTemplate(generation ? token : null);

  const rawBaseline = generation?.template?.content ?? null;
  const formattedBaseline = useMemo(
    () => (rawBaseline !== null ? formatTypstTemplate(rawBaseline) : null),
    [rawBaseline],
  );

  const [draft, setDraft] = useState<string | null>(null);
  useEffect(() => {
    setDraft(formattedBaseline);
  }, [formattedBaseline]);

  const template = generation?.template;
  return {
    draft,
    setDraft,
    /** "Nome vN" do template usado nesta geração. */
    templateLabel: template
      ? `Template ${template.name} v${template.version}`
      : "",
    dirty: draft !== null && draft !== formattedBaseline,
    canRestore: Boolean(bundled.data),
    restore() {
      if (!bundled.data) return;
      setDraft(
        bundled.data.content ? formatTypstTemplate(bundled.data.content) : null,
      );
    },
  };
}
