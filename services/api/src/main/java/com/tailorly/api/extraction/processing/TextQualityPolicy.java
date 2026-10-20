package com.tailorly.api.extraction.processing;

import java.text.Normalizer;
import org.springframework.stereotype.Component;

/**
 * Decide, página a página, se o texto extraído diretamente do PDF é bom o suficiente ou se a
 * página precisa ser renderizada e passar por OCR (veja {@link PdfExtractor#extract}).
 */
@Component
public class TextQualityPolicy {
  /** Normaliza acentuação (NFC) e quebras de linha antes de qualquer comparação ou persistência. */
  public String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFC)
        .replace("\r\n", "\n").replace('\r', '\n').strip();
  }

  /**
   * Heurística calibrada por fixtures de teste, não uma verificação de exatidão factual: aceita
   * texto nativo quando há pelo menos 40 caracteres visíveis, pelo menos 45% deles são letras
   * ou dígitos (evita aceitar ruído de extração de PDFs com fontes customizadas) e nenhum
   * caractere de substituição ({@code U+FFFD}) ou de controle inválido aparece no resultado.
   * Uma página que falha aqui é renderizada e recebe OCR.
   */
  public boolean accepts(String text) {
    long visible = text.codePoints().filter(c -> !Character.isWhitespace(c)).count();
    long lettersAndDigits = text.codePoints().filter(Character::isLetterOrDigit).count();
    long invalid = text.codePoints().filter(c -> c == 0xfffd || (Character.isISOControl(c)
        && !Character.isWhitespace(c))).count();
    return visible >= 40 && (double) lettersAndDigits / visible >= 0.45 && invalid == 0;
  }
}
