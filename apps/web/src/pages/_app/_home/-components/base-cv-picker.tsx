import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { useSelectBaseDocumentFile } from "@queries/base-document";
import { FileText, FileUp, RefreshCw } from "@react-zero-ui/icon-sprite";
import { useAuthStore } from "@stores/auth-store";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useReducedMotion } from "motion/react";
import { useRef, useState } from "react";

export function BaseCvPicker() {
  const reducedMotion = useReducedMotion();
  const accessToken = useAuthStore((state) => state.accessToken);
  const {
    file,
    error,
    selectionId,
    uploadedSelectionId,
    selectFile: onFileChange,
    setError,
  } = useBaseCvStore();
  const { selectFile, upload: uploadBaseDocument } =
    useSelectBaseDocumentFile();
  const currentUpload = file !== null && uploadBaseDocument.variables === file;
  const fileStatus = !accessToken
    ? "Prévia local • entre na sua conta para enviar"
    : currentUpload && uploadBaseDocument.isPending
      ? "Enviando PDF..."
      : selectionId && uploadedSelectionId === selectionId
        ? "Arquivo enviado • ainda não processado"
        : currentUpload && uploadBaseDocument.isError
          ? "Falha no envio • prévia local"
          : "Prévia local • ainda não enviado";

  function selectFiles(files: FileList | null) {
    if (!files?.length) return;
    if (files.length !== 1) {
      setError("Selecione um único arquivo PDF.");
      return;
    }

    selectFile(files[0]);
  }

  return (
    <section aria-labelledby="base-cv-heading" className="space-y-2">
      <input
        id="base-cv-file"
        type="file"
        accept=".pdf,application/pdf"
        aria-label="Selecionar currículo base"
        aria-describedby={error ? "base-cv-error" : undefined}
        className="peer sr-only"
        onChange={(event) => {
          selectFiles(event.target.files);
          event.target.value = "";
        }}
      />

      <AutoHeight
        deps={[file, error, fileStatus]}
        transition={reducedMotion ? { duration: 0 } : undefined}
        className="peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary-900"
      >
        <div className="space-y-2">
          {file ? (
            <FilePreview
              file={file}
              status={fileStatus}
              onFileChange={onFileChange}
              setError={setError}
            />
          ) : (
            <Picker selectFiles={selectFiles} />
          )}

          {error && (
            <p
              id="base-cv-error"
              role="alert"
              className="text-xs text-secondary-700"
            >
              {error}
            </p>
          )}
        </div>
      </AutoHeight>
    </section>
  );
}

interface FilePreviewProps {
  file: File;
  status: string;
  onFileChange: (file: File | null) => void;
  setError: (error: string | null) => void;
}

function FilePreview({
  file,
  status,
  onFileChange,
  setError,
}: FilePreviewProps) {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-outline-variant/40 bg-surface-container-low p-3">
      <div className="flex min-w-0 items-start gap-3">
        <FileText className="mt-0.5 size-5 shrink-0 text-primary-900" />
        <div className="flex min-w-0 flex-1 flex-col">
          <span
            role="status"
            className="mb-2 w-fit rounded-full border border-secondary-200 bg-secondary-container/40 px-2 text-[10px] font-semibold leading-4 tracking-[0.3px] text-secondary-800"
          >
            {status}
          </span>
          <p
            className="truncate text-sm font-semibold text-primary-950"
            title={file.name}
          >
            {file.name}
          </p>
          <p className="text-xs text-on-surface-variant">
            Currículo base selecionado
          </p>
        </div>
      </div>
      <div className="flex items-center justify-end border-t border-outline-variant/30 pt-2 ml-auto w-full gap-4 mt-4">
        <label
          htmlFor="base-cv-file"
          className="inline-flex cursor-pointer items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container"
        >
          <RefreshCw className="size-3.5" />
          Trocar
        </label>
        <button
          type="button"
          onClick={() => {
            onFileChange(null);
            setError(null);
          }}
          className="rounded-md px-2 py-1 text-xs font-semibold text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
        >
          Remover
        </button>
      </div>
    </div>
  );
}

interface PickerProps {
  selectFiles: (files: FileList | null) => void;
}

function Picker({ selectFiles }: PickerProps) {
  const [dragging, setDragging] = useState(false);
  const dragDepth = useRef(0);

  return (
    <label
      htmlFor="base-cv-file"
      onDragEnter={(event) => {
        event.preventDefault();
        dragDepth.current += 1;
        setDragging(true);
      }}
      onDragOver={(event) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = "copy";
      }}
      onDragLeave={(event) => {
        event.preventDefault();
        dragDepth.current -= 1;
        if (dragDepth.current <= 0) {
          dragDepth.current = 0;
          setDragging(false);
        }
      }}
      onDrop={(event) => {
        event.preventDefault();
        dragDepth.current = 0;
        setDragging(false);
        selectFiles(event.dataTransfer.files);
      }}
      className={`flex min-h-24 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed p-3 text-center transition-colors ${dragging ? "border-primary-900 bg-primary-50" : "border-outline-variant/60 bg-surface-container-low hover:border-primary-300 hover:bg-surface-container"}`}
    >
      <FileUp className="size-5 text-primary-900" />
      <span className="text-sm font-semibold text-primary-950">
        Arraste seu currículo aqui ou clique para selecionar
      </span>
      <span className="text-xs text-on-surface-variant">PDF de até 7 MiB</span>
    </label>
  );
}
