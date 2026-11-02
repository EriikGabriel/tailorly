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
import { apiErrorMessage } from "@lib/api/error-message";
import {
  useDeleteDueDiligence,
  useDueDiligenceHistory,
} from "@queries/due-diligence";
import { History, X } from "@react-zero-ui/icon-sprite";

const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

interface DueDiligenceHistoryProps {
  selectedId: string | null;
  onSelect: (id: string | null) => void;
}

/** Análises já feitas, guardadas na conta (ou na sessão anônima, que expira), para consulta posterior. */
export function DueDiligenceHistory({
  selectedId,
  onSelect,
}: DueDiligenceHistoryProps) {
  const history = useDueDiligenceHistory();
  const remove = useDeleteDueDiligence();
  const items = history.data ?? [];

  return (
    <div className="flex flex-col gap-3 rounded-xl bg-card p-4 shadow-xs">
      <h2 className="flex items-center gap-2 text-base font-semibold text-primary-950">
        <History aria-hidden="true" className="size-4" />
        Minhas análises
      </h2>

      {history.isLoading && (
        <p className="text-sm text-on-surface-variant">Carregando...</p>
      )}
      {history.isError && (
        <p role="alert" className="text-sm text-error">
          Não foi possível carregar suas análises.
        </p>
      )}
      {history.isSuccess && items.length === 0 && (
        <p className="text-sm text-on-surface-variant">
          Nenhuma análise ainda. As que você fizer ficam salvas aqui para
          consulta posterior.
        </p>
      )}

      <ul className="space-y-1">
        {items.map((item) => (
          <li
            key={item.id}
            className="flex items-center gap-1 rounded-lg data-[active=true]:bg-surface-container"
            data-active={item.id === selectedId}
          >
            <button
              type="button"
              onClick={() => onSelect(item.id)}
              aria-current={item.id === selectedId ? "true" : undefined}
              className="min-w-0 flex-1 rounded-lg px-3 py-2 text-left hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
            >
              <span className="block truncate text-sm font-semibold text-primary-950">
                {item.companyName}
              </span>
              <span className="block truncate text-xs text-on-surface-variant">
                {dateFormat.format(new Date(item.createdAt))} ·{" "}
                {item.status === "running"
                  ? "em andamento"
                  : item.status === "failed"
                    ? "não concluída"
                    : `${item.factorsWithEvidence}/${item.factorsTotal} fatores com fontes`}
                {item.triggerKind === "pipeline" ? " · automática" : ""}
                {item.jobTitle ? ` · ${item.jobTitle}` : ""}
              </span>
            </button>
            <AlertDialog>
              <AlertDialogTrigger
                render={
                  <Button
                    type="button"
                    variant="ghost"
                    aria-label={`Excluir análise de ${item.companyName}`}
                    disabled={remove.isPending}
                    className="flex size-8 shrink-0 items-center justify-center rounded-lg text-primary-950"
                  >
                    <X strokeWidth={1.5} className="size-4" />
                  </Button>
                }
              />
              <AlertDialogPopup>
                <AlertDialogHeader>
                  <AlertDialogTitle>Excluir esta análise?</AlertDialogTitle>
                  <AlertDialogDescription>
                    O relatório de {item.companyName} é apagado e não pode ser
                    recuperado. O Currículo Base e os currículos gerados não são
                    afetados.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={() =>
                      remove.mutate(item.id, {
                        onSuccess: () => {
                          if (item.id === selectedId) onSelect(null);
                        },
                      })
                    }
                  >
                    Excluir
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogPopup>
            </AlertDialog>
          </li>
        ))}
      </ul>
      {remove.isError && (
        <p role="alert" className="text-sm text-error">
          {apiErrorMessage(remove.error, "Não foi possível excluir.")}
        </p>
      )}
    </div>
  );
}
