package com.tailorly.api.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultNoteReaderTest {
  @TempDir
  Path vault;

  @Test
  void findsMarkdownNotesThatMentionTheTerm() throws Exception {
    Files.createDirectories(vault.resolve("Projetos"));
    Files.writeString(vault.resolve("Projetos/tailorly.md"), "Tailorly usa   Typst para PDF.");
    Files.writeString(vault.resolve("Projetos/outro.md"), "Nada a ver.");
    Files.writeString(vault.resolve("Projetos/tailorly.txt"), "Tailorly em texto puro.");

    List<VaultNoteReader.Note> notes = new VaultNoteReader().findMentioning(vault, "tailorly");

    assertEquals(1, notes.size());
    assertEquals(Path.of("Projetos", "tailorly.md").toString(), notes.getFirst().key());
  }

  @Test
  void doesNothingWithoutAVault() {
    assertTrue(new VaultNoteReader().findMentioning(null, "Tailorly").isEmpty());
  }

  @Test
  void stopsAtTheMaximumNumberOfNotes() throws Exception {
    for (int i = 0; i < VaultNoteReader.MAX_NOTES + 2; i++) {
      Files.writeString(vault.resolve("nota-" + i + ".md"), "Tailorly " + i);
    }

    assertEquals(VaultNoteReader.MAX_NOTES, new VaultNoteReader().findMentioning(vault, "Tailorly").size());
  }
}
