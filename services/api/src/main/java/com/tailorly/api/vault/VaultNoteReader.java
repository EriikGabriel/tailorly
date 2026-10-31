package com.tailorly.api.vault;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.tailorly.api.shared.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lê notas Markdown do vault de uma conta para enriquecer projetos. Só lê dentro da pasta do vault
 * (já validada por {@link VaultLocations}) e não segue links simbólicos.
 */
@Component
public class VaultNoteReader {
  static final int MAX_FILES_SCANNED = 2000;
  static final long MAX_NOTE_BYTES = 200_000;
  static final int MAX_NOTES = 2;
  private static final Logger log = LoggerFactory.getLogger(VaultNoteReader.class);

  /** Uma nota: chave relativa à pasta do vault e texto completo (arquivos grandes são ignorados). */
  public record Note(String key, String text) {
  }

  /** Notas que citam o termo; no máximo {@link #MAX_NOTES}, varrendo no máximo {@link #MAX_FILES_SCANNED} arquivos. */
  public List<Note> findMentioning(Path vaultDir, String term) {
    List<Note> notes = new ArrayList<>();
    if (vaultDir == null || Text.normalize(term).isEmpty()) {
      return notes;
    }
    try (Stream<Path> stream = Files.walk(vaultDir)) {
      Iterator<Path> files = stream
          .filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
          .filter(file -> file.getFileName().toString().endsWith(".md")).iterator();
      int scanned = 0;
      while (files.hasNext() && notes.size() < MAX_NOTES && scanned < MAX_FILES_SCANNED) {
        Path file = files.next();
        scanned++;
        try {
          if (Files.size(file) > MAX_NOTE_BYTES) {
            continue;
          }
          String text = Files.readString(file, StandardCharsets.UTF_8);
          if (Text.mentions(text, term)) {
            notes.add(new Note(vaultDir.relativize(file).toString(), text));
          }
        } catch (IOException unreadable) {
          // Nota ilegível (por exemplo, não UTF-8) é ignorada; as demais continuam sendo lidas.
          log.debug("Vault note skipped: {}", unreadable.getClass().getSimpleName());
        }
      }
    } catch (IOException | UncheckedIOException failure) {
      log.warn("Vault could not be read: {}", failure.getClass().getSimpleName());
    }
    return notes;
  }
}
