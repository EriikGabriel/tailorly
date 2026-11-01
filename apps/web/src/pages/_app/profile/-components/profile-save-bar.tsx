import { Button } from "@animate/buttons/button";
import { useViewer } from "@queries/cv-generation";
import { useSaveGenerationDefaults } from "@queries/generation-defaults";
import {
  isOwnerVaultInputDirty,
  useOwnerVault,
  useSaveOwnerVault,
} from "@queries/owner-vault";
import { useProfileGenerationDefaults } from "@queries/profile-generation-defaults";
import { AlertTriangle, Info } from "@react-zero-ui/icon-sprite";
import { useOwnerVaultDraftStore } from "@stores/owner-vault-draft-store";
import { useProfileSettingsStore } from "@stores/profile-settings-store";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";

/**
 * Barra única de alterações não salvas da página de perfil. Aparece quando há edição pendente.
 **/
export function ProfileSaveBar() {
  const isAiDirty = useProfileSettingsStore((state) => state.isDirty);
  const isAiConfigValid = useProfileSettingsStore(
    (state) => state.isAiConfigValid,
  );
  const isAiSaving = useProfileSettingsStore((state) => state.isSaving);
  const aiSaveError = useProfileSettingsStore((state) => state.saveError);
  const saveAi = useProfileSettingsStore((state) => state.save);
  const discardAi = useProfileSettingsStore((state) => state.discard);

  const viewer = useViewer();
  const isAccount = viewer.kind === "account" && viewer.ownerId !== null;
  const vault = useOwnerVault(isAccount ? viewer.ownerId : null, viewer.token);
  const vaultDraft = useOwnerVaultDraftStore((state) => state.draft);
  const setVaultDraft = useOwnerVaultDraftStore((state) => state.setDraft);
  const saveVault = useSaveOwnerVault(viewer.ownerId, viewer.token);
  const isVaultDirty = isOwnerVaultInputDirty(vaultDraft, vault.data);

  const generation = useProfileGenerationDefaults();
  const saveGeneration = useSaveGenerationDefaults(
    viewer.ownerId,
    viewer.token,
  );

  const isDirty = isAiDirty || isVaultDirty || generation.isDirty;
  const isSaving =
    isAiSaving || saveVault.isPending || saveGeneration.isPending;
  const saveError =
    aiSaveError ??
    saveVault.error?.message ??
    saveGeneration.error?.message ??
    null;
  const reduceMotion = useReducedMotion();

  function handleSave() {
    if (isAiDirty) saveAi().catch(() => {});
    if (isVaultDirty && vaultDraft) {
      saveVault.mutate(vaultDraft, { onSuccess: () => setVaultDraft(null) });
    }
    if (generation.isDirty && generation.draft) {
      saveGeneration.mutate(generation.draft, {
        onSuccess: () => generation.setDraft(null),
      });
    }
  }

  function handleDiscard() {
    discardAi();
    setVaultDraft(null);
    generation.setDraft(null);
  }

  return (
    <AnimatePresence>
      {isDirty && (
        <motion.div
          role="region"
          aria-label="Alterações não salvas"
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={reduceMotion ? undefined : { opacity: 0, y: 12 }}
          transition={{ type: "spring", stiffness: 400, damping: 30 }}
          className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-container border-2 px-4 py-4 shadow-2"
        >
          {saveError ? (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-destructive">
              <AlertTriangle aria-hidden="true" className="size-4 shrink-0" />
              {saveError}
            </p>
          ) : isAiDirty && !isAiConfigValid ? (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-destructive">
              <AlertTriangle aria-hidden="true" className="size-4 shrink-0" />
              Config. de IA incompleta ou inválida — corrija endpoint, modelo e
              chave antes de salvar.
            </p>
          ) : (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-on-surface-variant">
              <Info aria-hidden="true" className="size-4 shrink-0" />
              Todas as alterações de configurações respeitam os limites da
              criptografia local.
            </p>
          )}
          <div className="flex items-center gap-2">
            <Button variant="ghost" onClick={handleDiscard} disabled={isSaving}>
              Descartar
            </Button>
            <Button
              onClick={handleSave}
              disabled={isSaving || (isAiDirty && !isAiConfigValid)}
            >
              {isSaving ? "Salvando..." : "Salvar Alterações"}
            </Button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
