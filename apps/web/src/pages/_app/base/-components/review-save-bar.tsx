import { Button } from "@animate/buttons/button";
import { AlertTriangle, Info } from "@react-zero-ui/icon-sprite";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";

type ReviewSaveBarProps = {
  visible: boolean;
  saving: boolean;
  approving: boolean;
  disabled: boolean;
  error: string | null;
  onSave: () => void;
  onApprove: () => void;
  onDiscard: () => void;
};

/**
 * Barra flutuante que aparece assim que há edição não gravada na auditoria, como a do perfil.
 * "Salvar e aprovar" também atualiza o Ground Truth ativo; salvar só grava o rascunho, e as
 * gerações continuam lendo o último snapshot aprovado.
 */
export function ReviewSaveBar({
  visible,
  saving,
  approving,
  disabled,
  error,
  onSave,
  onApprove,
  onDiscard,
}: ReviewSaveBarProps) {
  const reduceMotion = useReducedMotion();
  const busy = saving || approving;

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          role="region"
          aria-label="Alterações não salvas"
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={reduceMotion ? undefined : { opacity: 0, y: 12 }}
          transition={{ type: "spring", stiffness: 400, damping: 30 }}
          className="sticky bottom-4 z-10 mx-auto mt-6 flex w-full max-w-384 flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-container border-2 px-4 py-4 shadow-2"
        >
          {error ? (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-destructive">
              <AlertTriangle aria-hidden="true" className="size-4 shrink-0" />
              {error}
            </p>
          ) : (
            <p className="flex items-center gap-2 text-label-sm font-semibold text-on-surface-variant">
              <Info aria-hidden="true" className="size-4 shrink-0" />
              Você tem alterações não salvas. Os currículos só usam o Ground
              Truth depois de aprovado.
            </p>
          )}
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="ghost" onClick={onDiscard} disabled={busy}>
              Descartar
            </Button>
            <Button
              variant="accent"
              onClick={onSave}
              disabled={disabled || busy}
            >
              {saving ? "Salvando..." : "Salvar rascunho"}
            </Button>
            <Button onClick={onApprove} disabled={disabled || busy}>
              {approving ? "Aprovando..." : "Salvar e aprovar"}
            </Button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
