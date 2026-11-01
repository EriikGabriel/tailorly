package com.tailorly.api.extraction.run;

/** Etapas que o worker informa enquanto processa uma execução; a interface traduz cada uma em texto. */
public final class ExtractionStage {
  public static final String READING_FILE = "READING_FILE";
  public static final String EXTRACTING_TEXT = "EXTRACTING_TEXT";
  public static final String SAVING_PAGES = "SAVING_PAGES";
  public static final String INTERPRETING = "INTERPRETING";
  public static final String SAVING_FACTS = "SAVING_FACTS";

  private ExtractionStage() {
  }
}
