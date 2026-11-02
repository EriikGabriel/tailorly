/** Mensagens da falha da extração do Currículo Base, por código do servidor. */
const FAILURES: Record<string, string> = {
  AI_INTERPRETATION_FAILED:
    "A IA não conseguiu interpretar o currículo. Confira a configuração de IA no perfil e envie o PDF de novo.",
  PDF_NO_TEXT: "O PDF não tem texto legível, nem por OCR.",
  PDF_ENCRYPTED: "O PDF está protegido por senha.",
  OCR_UNAVAILABLE: "A leitura por OCR não está disponível no servidor.",
  WORKER_TIMEOUT: "O processamento demorou demais e foi interrompido.",
  STORAGE_READ_FAILED: "Não foi possível abrir o arquivo enviado.",
  STORAGE_WRITE_FAILED: "Não foi possível guardar o texto extraído.",
  EXTRACTION_FAILED: "Não foi possível extrair o texto do PDF.",
  EXTRACTION_PERSIST_FAILED: "Não foi possível registrar as páginas extraídas.",
  FACT_FINALIZATION_FAILED:
    "Não foi possível registrar os fatos interpretados.",
};

export function extractionFailureMessage(errorCode?: string | null): string {
  return (
    FAILURES[errorCode ?? ""] ??
    `Falha na extração${errorCode ? ` (${errorCode})` : ""}.`
  );
}
