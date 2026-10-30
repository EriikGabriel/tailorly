import type { CvGenerationResponse } from "@lib/api/generated/model";
import { cvTemplateFindById } from "@lib/api/generated/tailorly";
import { useBundledCvTemplate } from "@queries/cv-generation";
import { skipToken, useQuery } from "@tanstack/react-query";
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
  const templateId = generation?.template?.id;
  const template = useQuery({
    queryKey: ["cv", "template", "by-id", templateId ?? ""],
    queryFn:
      templateId && token
        ? () => cvTemplateFindById(templateId, { accessToken: token })
        : skipToken,
    staleTime: Number.POSITIVE_INFINITY,
  });
  const bundled = useBundledCvTemplate(token);

  const rawBaseline =
    generation?.template?.content ?? template.data?.content ?? null;
  const formattedBaseline = useMemo(
    () => (rawBaseline !== null ? formatTypstTemplate(rawBaseline) : null),
    [rawBaseline],
  );

  const [draft, setDraft] = useState<string | null>(null);
  useEffect(() => {
    setDraft(formattedBaseline);
  }, [formattedBaseline]);

  return {
    draft,
    setDraft,
    template,
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
