import { Button } from "@components/ui/animate/buttons/button";
import {
  DialogDescription,
  DialogTitle,
} from "@components/ui/animate/primitives/base/dialog";
import { ListTodo, X } from "@react-zero-ui/icon-sprite";
import {
  quickReviewDescription,
  quickReviewHeading,
} from "@/utils/quick-review-copy";

/** Cabeçalho do diálogo: título (muda com a priorização), contador de pendências e botão de fechar. */
export function DialogHeader({
  count,
  prioritized,
  onClose,
}: {
  count: number;
  prioritized: boolean;
  onClose: () => void;
}) {
  return (
    <div className="shrink-0 border-b border-outline-variant/30 bg-surface-container-low p-5 sm:px-7">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-2">
          <DialogTitle className="flex items-center gap-2 text-lg font-semibold leading-6">
            <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-white">
              <ListTodo className="size-4.5" />
            </span>
            {quickReviewHeading(count, prioritized)}
          </DialogTitle>
          <span className="w-fit rounded-full border bg-white px-3 py-0.5 text-[10px] font-bold uppercase leading-4 tracking-[0.3px] text-tertiary">
            {`${count} pendências para revisar`}
          </span>
          <DialogDescription className="text-sm leading-5 text-on-surface-variant">
            {quickReviewDescription(prioritized)}
          </DialogDescription>
        </div>
        <Button
          variant="ghost"
          type="button"
          onClick={onClose}
          aria-label="Fechar validação"
          className="rounded-md p-2 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
        >
          <X className="size-4" />
        </Button>
      </div>
    </div>
  );
}
