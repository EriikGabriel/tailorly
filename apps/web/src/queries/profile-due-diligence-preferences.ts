import { useViewer } from "@queries/cv-generation";
import {
  useDueDiligencePreferences,
  useSaveDueDiligencePreferences,
} from "@queries/due-diligence-preferences";
import { useDueDiligenceDraftStore } from "@stores/due-diligence-draft-store";

/**
 * Interruptor de due diligence no perfil: valor salvo no servidor (ligado até a conta mudar), rascunho
 * ainda não salvo e se há diferença entre os dois. A gravação acontece pela barra de alterações.
 */
export function useProfileDueDiligencePreferences() {
  const viewer = useViewer();
  const ownerId = viewer.kind === "account" ? viewer.ownerId : null;
  const query = useDueDiligencePreferences(ownerId, viewer.token);
  const draft = useDueDiligenceDraftStore((state) => state.draft);
  const setDraft = useDueDiligenceDraftStore((state) => state.setDraft);
  const save = useSaveDueDiligencePreferences(ownerId, viewer.token);

  const saved = query.data?.pipelineEnabled ?? true;
  const enabled = draft ?? saved;

  return {
    isAccount: ownerId !== null,
    isLoading: query.isLoading,
    enabled,
    saved,
    draft,
    setDraft,
    isDirty: draft !== null && draft !== saved,
    save,
  };
}
