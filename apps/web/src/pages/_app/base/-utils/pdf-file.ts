const PDF_NAME_PATTERN = /\.pdf$/i;

/** A visualização com destaques só existe para PDFs; outros formatos usam a alternativa textual. */
export function isPdfFile(file: File | null): file is File {
  return Boolean(file && PDF_NAME_PATTERN.test(file.name));
}
