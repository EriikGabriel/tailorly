import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@animate/base/alert-dialog";
import { Button } from "@animate/buttons/button";
import {
  CircleCheck,
  FileUp,
  PowerOff,
  Trash2,
} from "@react-zero-ui/icon-sprite";

import type { usePdfFilePicker } from "../-hooks/use-pdf-file-picker";

/** Ações do cabeçalho: escolher o PDF do Currículo Base, aprovar, desativar e excluir a versão. */
export function BaseDocumentActions({
  picker,
  hasFile,
  canApprove,
  onApprove,
  canDeactivate,
  onDeactivate,
  deactivating,
  canDelete,
  onDelete,
  deleting,
}: {
  picker: ReturnType<typeof usePdfFilePicker>;
  hasFile: boolean;
  canApprove: boolean;
  onApprove: () => void;
  canDeactivate: boolean;
  onDeactivate: () => void;
  deactivating: boolean;
  canDelete: boolean;
  onDelete: () => void;
  deleting: boolean;
}) {
  return (
    <div className="flex flex-col gap-2 xl:max-w-[52%] xl:items-end">
      <input
        ref={picker.inputRef}
        type="file"
        accept=".pdf,application/pdf"
        aria-label="Selecionar PDF do Currículo Base"
        className="hidden"
        onChange={picker.onChange}
      />
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap xl:justify-end">
        <Button
          variant="accent"
          onClick={picker.open}
          className="h-auto min-h-12 min-w-60 gap-1 rounded-xl bg-card px-6 py-3.5 text-label-md font-semibold text-primary-950 hover:bg-surface-container-high has-[>svg]:px-6"
        >
          <FileUp aria-hidden="true" className="size-4" />
          {hasFile ? "Selecionar outro PDF" : "Selecionar Currículo Base"}
        </Button>
        <Button
          disabled={!canApprove}
          onClick={onApprove}
          aria-describedby="base-availability"
          className="h-auto min-h-12 min-w-60 gap-1 whitespace-normal text-white! disabled:text-white rounded-xl px-6 py-3.5 text-label-md font-semibold shadow-md has-[>svg]:px-6"
        >
          <CircleCheck aria-hidden="true" className="size-4" />
          Aprovar currículo base
        </Button>
      </div>
      {(canDeactivate || canDelete) && (
        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap xl:justify-end">
          {canDeactivate && (
            <AlertDialog>
              <AlertDialogTrigger
                render={
                  <Button
                    variant="outline"
                    disabled={deactivating}
                    className="h-auto min-h-12 min-w-60 gap-1 rounded-xl px-6 py-3.5 text-label-md font-semibold has-[>svg]:px-6"
                  >
                    <PowerOff aria-hidden="true" className="size-4" />
                    Desativar Currículo Base
                  </Button>
                }
              />
              <AlertDialogPopup>
                <AlertDialogHeader>
                  <AlertDialogTitle>
                    Desativar o Ground Truth ativo?
                  </AlertDialogTitle>
                  <AlertDialogDescription>
                    A conta fica sem Currículo Base ativo até você aprovar uma
                    nova versão. Nada é apagado: fatos, pendências e os
                    currículos já gerados continuam disponíveis para auditoria.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction onClick={onDeactivate}>
                    Desativar
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogPopup>
            </AlertDialog>
          )}
          {canDelete && (
            <AlertDialog>
              <AlertDialogTrigger
                render={
                  <Button
                    variant="outline"
                    disabled={deleting}
                    className="h-auto min-h-12 min-w-60 gap-1 rounded-xl px-6 py-3.5 text-label-md font-semibold text-error has-[>svg]:px-6"
                  >
                    <Trash2 aria-hidden="true" className="size-4" />
                    Excluir Currículo Base
                  </Button>
                }
              />
              <AlertDialogPopup>
                <AlertDialogHeader>
                  <AlertDialogTitle>
                    Excluir esta versão do Currículo Base?
                  </AlertDialogTitle>
                  <AlertDialogDescription>
                    O arquivo, o texto extraído, os fatos e as pendências desta
                    versão são apagados e não podem ser recuperados. Currículos
                    já gerados a partir dela também são apagados, porque contêm
                    os mesmos dados pessoais.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction onClick={onDelete}>
                    Excluir
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogPopup>
            </AlertDialog>
          )}
        </div>
      )}
    </div>
  );
}
