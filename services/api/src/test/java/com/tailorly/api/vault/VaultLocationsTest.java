package com.tailorly.api.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultLocationsTest {
  @TempDir
  Path tmp;

  private Path root;

  private VaultLocations locations() throws Exception {
    root = Files.createDirectories(tmp.resolve("raiz"));
    Files.createDirectories(root.resolve("Pessoal/Second Brain"));
    return new VaultLocations(root.toString());
  }

  @Test
  void acceptsAFolderInsideTheRoot() throws Exception {
    Path resolved = locations().resolve("Pessoal/Second Brain");

    assertEquals(root.resolve("Pessoal/Second Brain").toRealPath(), resolved);
  }

  @Test
  void rejectsParentSegmentsAbsolutePathsAndFiles() throws Exception {
    VaultLocations locations = locations();
    Files.writeString(root.resolve("arquivo.md"), "x");

    assertInvalid(locations, "../fora");
    assertInvalid(locations, "Pessoal/../../fora");
    assertInvalid(locations, tmp.toString());
    assertInvalid(locations, "arquivo.md");
    assertInvalid(locations, "  ");
    assertInvalid(locations, null);
  }

  @Test
  void rejectsSymbolicLinksThatLeaveTheRoot() throws Exception {
    VaultLocations locations = locations();
    Path outside = Files.createDirectories(tmp.resolve("fora"));
    Files.createSymbolicLink(root.resolve("atalho"), outside);

    assertInvalid(locations, "atalho");
  }

  @Test
  void reportsUnavailableWhenNoRootIsConfigured() {
    VaultLocations locations = new VaultLocations("");

    assertFalse(locations.isAvailable());
    BadRequestException error = assertThrows(BadRequestException.class, () -> locations.resolve("qualquer"));
    assertEquals(ErrorCodes.Vault.NOT_CONFIGURED, error.getMessage());
    assertTrue(locations.tryResolve("qualquer").isEmpty());
  }

  private static void assertInvalid(VaultLocations locations, String path) {
    BadRequestException error = assertThrows(BadRequestException.class, () -> locations.resolve(path));
    assertEquals(ErrorCodes.Vault.INVALID_PATH, error.getMessage());
  }
}
