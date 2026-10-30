import { Button } from "@animate/buttons/button";
import { AlertTriangle, Info } from "@react-zero-ui/icon-sprite";
import { useProfileSettingsStore } from "@stores/profile-settings-store";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";

export function ProfileSaveBar() {
  const isDirty = useProfileSettingsStore((state) => state.isDirty);
  const isAiConfigValid = useProfileSettingsStore(
    (state) => state.isAiConfigValid,
  );
  const isSaving = useProfileSettingsStore((state) => state.isSaving);
  const saveError = useProfileSettingsStore((state) => state.saveError);
  const save = useProfileSettingsStore((state) => state.save);
  const discard = useProfileSettingsStore((state) => state.discard);
  const reduceMotion = useReducedMotion();

  function handleSave() {
    save().catch(() => {
      // O erro já fica disponível em saveError para exibição na barra.
    });
  }

  return (
    <AnimatePresence>
      {isDirty && (
        <motion.div
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={reduceMotion ? undefined : { opacity: 0, y: 12 }}
          transition={{ type: "spring", stiffness: 400, damping: 30 }}
          className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-container px-4 py-4 shadow-2"
        >
          {saveError ? (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-destructive">
              <AlertTriangle aria-hidden="true" className="size-4 shrink-0" />
              {saveError}
            </p>
          ) : !isAiConfigValid ? (
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
            <Button variant="ghost" onClick={discard} disabled={isSaving}>
              Descartar
            </Button>
            <Button
              onClick={handleSave}
              disabled={isSaving || !isAiConfigValid}
            >
              {isSaving ? "Salvando..." : "Salvar Alterações"}
            </Button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
