import { useViewer } from "@queries/cv-generation";
import { useGenerationDefaults } from "@queries/generation-defaults";
import {
  defaultGenerationDraft,
  draftFromSaved,
  type GenerationDefaultsDraft,
  isGenerationDraftDirty,
  useGenerationDefaultsDraftStore,
} from "@stores/generation-defaults-draft-store";

/**
 * Padrões de geração da conta no perfil: o valor salvo no servidor (ATS ligado e diretrizes padrão
 * até a conta mudar), o rascunho ainda não salvo e se há diferença entre os dois.
 */
export function useProfileGenerationDefaults() {
  const viewer = useViewer();
  const accountOwnerId = viewer.kind === "account" ? viewer.ownerId : null;
  const defaults = useGenerationDefaults(accountOwnerId, viewer.token);
  const draft = useGenerationDefaultsDraftStore((state) => state.draft);
  const setDraft = useGenerationDefaultsDraftStore((state) => state.setDraft);

  const saved: GenerationDefaultsDraft | undefined = defaults.data
    ? draftFromSaved(defaults.data)
    : undefined;
  const current = draft ?? saved ?? defaultGenerationDraft();

  return {
    isAccount: accountOwnerId !== null,
    isLoading: defaults.isLoading,
    saved,
    current,
    draft,
    setDraft,
    isDirty: isGenerationDraftDirty(draft, saved),
  };
}
