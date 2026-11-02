import { Button } from "@animate/buttons/button";
import {
  Dialog,
  DialogBackdrop,
  DialogDescription,
  DialogPopup,
  DialogPortal,
  DialogTitle,
} from "@animate/primitives/base/dialog";
import { X } from "@react-zero-ui/icon-sprite";
import type { ReactNode } from "react";

/**
 * Diálogo para listas e detalhes longos que não cabem na coluna: a tela mostra só um resumo e o
 * restante fica aqui, com rolagem própria. O conteúdo é montado só quando está aberto.
 */
export function DetailsDialog({
  open,
  onOpenChange,
  title,
  description,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65 backdrop-blur-sm" />
        <DialogPopup
          className="flex max-h-[min(88dvh,720px)] w-[min(720px,calc(100vw-2rem))] max-w-none flex-col gap-0 overflow-hidden rounded-xl border border-primary-200 bg-card p-0 text-primary-950 shadow-2xl"
          style={{
            position: "fixed",
            top: "50%",
            left: "50%",
            translate: "-50% -50%",
            zIndex: 51,
          }}
        >
          <div className="flex shrink-0 items-start justify-between gap-3 border-b border-outline-variant/30 bg-surface-container-low p-5">
            <div className="flex flex-col gap-1">
              <DialogTitle className="text-lg font-semibold leading-6">
                {title}
              </DialogTitle>
              {description && (
                <DialogDescription className="text-sm leading-5 text-on-surface-variant">
                  {description}
                </DialogDescription>
              )}
            </div>
            <Button
              variant="ghost"
              type="button"
              onClick={() => onOpenChange(false)}
              aria-label="Fechar"
              className="rounded-md p-2 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
            >
              <X className="size-4" />
            </Button>
          </div>
          <div className="min-h-0 overflow-y-auto overscroll-contain p-5">
            {children}
          </div>
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}
