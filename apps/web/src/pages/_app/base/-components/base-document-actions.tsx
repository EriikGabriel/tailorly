import { Button } from "@animate/buttons/button";
import { CircleCheck, FileUp } from "@react-zero-ui/icon-sprite";
import type { usePdfFilePicker } from "../-hooks/use-pdf-file-picker";

/** Ações do cabeçalho: escolher o PDF do Currículo Base e aprovar a versão revisada. */
export function BaseDocumentActions({
  picker,
  hasFile,
  canApprove,
  onApprove,
}: {
  picker: ReturnType<typeof usePdfFilePicker>;
  hasFile: boolean;
  canApprove: boolean;
  onApprove: () => void;
}) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap xl:max-w-[52%] xl:justify-end">
      <input
        ref={picker.inputRef}
        type="file"
        accept=".pdf,application/pdf"
        aria-label="Selecionar PDF do Currículo Base"
        className="hidden"
        onChange={picker.onChange}
      />
      <Button
        variant="accent"
        onClick={picker.open}
        className="h-auto min-h-12 gap-1 rounded-xl bg-card px-6 py-3.5 text-label-md font-semibold text-primary-950 hover:bg-surface-container-high has-[>svg]:px-6"
      >
        <FileUp aria-hidden="true" className="size-4" />
        {hasFile ? "Selecionar outro PDF" : "Selecionar Currículo Base"}
      </Button>
      <Button
        disabled={!canApprove}
        onClick={onApprove}
        aria-describedby="base-availability"
        className="h-auto min-h-12 gap-1 whitespace-normal text-white! disabled:text-white rounded-xl px-6 py-3.5 text-label-md font-semibold shadow-md has-[>svg]:px-6"
      >
        <CircleCheck aria-hidden="true" className="size-4" />
        Aprovar currículo base
      </Button>
    </div>
  );
}
