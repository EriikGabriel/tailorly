package com.tailorly.api.cv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.tailorly.api.cv.ats.AtsEvaluator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** PDFs de teste com texto real, no lugar do Typst, para que a avaliação de ATS leia algo de verdade. */
public final class TestPdfs {
  private TestPdfs() {
  }

  public static byte[] of(String text, int linesPerPage) {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      String[] lines = text.split("\n");
      int index = 0;
      do {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
          stream.beginText();
          stream.setFont(font, 8);
          stream.setLeading(10);
          stream.newLineAtOffset(40, 780);
          for (int line = 0; line < linesPerPage && index < lines.length; line++, index++) {
            stream.showText(lines[index].replaceAll("[^\\x20-\\x7E\\u00C0-\\u00FF]", "?"));
            stream.newLine();
          }
          stream.endText();
        }
      } while (index < lines.length);
      document.save(out);
      return out.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public static byte[] of(com.tailorly.api.cv.model.CvDocument document) {
    return of(AtsEvaluator.flatten(document), 70);
  }
}
