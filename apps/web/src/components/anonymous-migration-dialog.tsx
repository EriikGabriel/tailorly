import {
  Dialog,
  DialogDescription,
  DialogHeader,
  DialogPopup,
  DialogTitle,
} from "@animate/base/dialog";
import { Button } from "@animate/buttons/button";
import { DialogBackdrop, DialogPortal } from "@animate/primitives/base/dialog";
import { ApiClientError } from "@lib/api/client";
import {
  useAnonymousSessionStatus,
  useDiscardAnonymousSession,
  useMigrateAnonymousSession,
} from "@queries/anonymous-session";
import { useAnonymousSessionStore } from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";
import { useEffect, useState } from "react";

/**
 * Depois do login, oferece trazer para a conta o currículo que o visitante já revisou e
 * aprovou, sem repetir OCR. Sem migrar, os dados do visitante expiram no servidor.
 */
export function AnonymousMigrationDialog() {
  const accountToken = useAuthStore((state) => state.accessToken);
  const sessionId = useAnonymousSessionStore(
    (state) => state.session?.sessionId,
  );
  const status = useAnonymousSessionStatus();
  const migrate = useMigrateAnonymousSession();
  const discard = useDiscardAnonymousSession();
  const [dismissed, setDismissed] = useState(false);
  const [needsReplaceConfirmation, setNeedsReplaceConfirmation] =
    useState(false);

  // biome-ignore lint/correctness/useExhaustiveDependencies: uma nova sessão ou login reabre a oferta.
  useEffect(() => {
    setDismissed(false);
    setNeedsReplaceConfirmation(false);
  }, [accountToken, sessionId]);

  const open = Boolean(
    accountToken && sessionId && status.data?.migratable && !dismissed,
  );
  const pending = migrate.isPending || discard.isPending;
  const error = migrate.error ?? discard.error;

  function runMigration(replaceActive: boolean) {
    migrate.mutate(
      { replaceActive },
      {
        onError: (cause) => {
          // 409: a conta já tem um Ground Truth ativo; só substitui com confirmação explícita.
          if (cause instanceof ApiClientError && cause.status === 409) {
            setNeedsReplaceConfirmation(true);
            migrate.reset();
          }
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && setDismissed(true)}>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65 backdrop-blur-sm" />
        <DialogPopup className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Trazer seu currículo para a conta?</DialogTitle>
            <DialogDescription>
              {needsReplaceConfirmation
                ? "Sua conta já tem um Ground Truth ativo. Se continuar, o currículo de visitante passa a ser o ativo e o anterior fica arquivado."
                : "Você revisou e aprovou um currículo como visitante. Ele pode ser movido para esta conta sem precisar processar o PDF de novo. Se não trouxer, os dados de visitante são apagados quando a sessão expirar."}
            </DialogDescription>
          </DialogHeader>

          {error && (
            <p
              role="alert"
              className="rounded-md bg-error-container px-3 py-2 text-sm text-on-error-container"
            >
              {error.message}
            </p>
          )}

          <div className="flex flex-wrap justify-end gap-2">
            <Button
              type="button"
              variant="ghost"
              disabled={pending}
              onClick={() => discard.mutate()}
            >
              Descartar currículo de visitante
            </Button>
            <Button
              type="button"
              disabled={pending}
              onClick={() => runMigration(needsReplaceConfirmation)}
            >
              {pending
                ? "Aguarde..."
                : needsReplaceConfirmation
                  ? "Substituir e trazer"
                  : "Trazer para minha conta"}
            </Button>
          </div>
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}
