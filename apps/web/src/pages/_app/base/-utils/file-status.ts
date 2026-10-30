type FileStatusInput = {
  file: File | null;
  /** O arquivo atual está sendo enviado agora. */
  uploadPending: boolean;
  selectionId: string | null;
  uploadedSelectionId: string | null;
};

/** Rótulo do estado do arquivo mestre: aguardando, prévia local, enviando ou enviado. */
export function baseFileStatus({
  file,
  uploadPending,
  selectionId,
  uploadedSelectionId,
}: FileStatusInput): string {
  if (uploadPending) return "Enviando PDF...";
  if (!file) return "Aguardando Currículo Base";
  return selectionId && uploadedSelectionId === selectionId
    ? "PDF enviado"
    : "Prévia local • ainda não enviado";
}

/** Tamanho do arquivo em KB, no formato brasileiro (ex.: "284,3 KB"). */
export function formatFileSizeKb(file: File): string {
  return `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(file.size / 1024)} KB`;
}
