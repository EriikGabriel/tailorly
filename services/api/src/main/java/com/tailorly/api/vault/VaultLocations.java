package com.tailorly.api.vault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida os caminhos de vault escolhidos pelas contas. Todo vault precisa ser uma pasta existente
 * dentro de {@code tailorly.vault.root}, em caminho relativo, sem {@code ..} e sem links simbólicos
 * que saiam da raiz. Assim uma conta não consegue apontar o servidor para arquivos fora da pasta
 * permitida.
 */
@Component
public class VaultLocations {
  private static final Logger log = LoggerFactory.getLogger(VaultLocations.class);

  private final Path root;

  public VaultLocations(@Value("${tailorly.vault.root:}") String rootPath) {
    Path real = null;
    if (rootPath != null && !rootPath.isBlank()) {
      try {
        real = Path.of(rootPath).toRealPath();
      } catch (IOException missing) {
        log.warn("tailorly.vault.root does not exist; vault features are disabled");
      }
    }
    this.root = real;
  }

  public boolean isAvailable() {
    return root != null;
  }

  /** Diretório real do vault, ou {@link BadRequestException} se o caminho não for válido. */
  public Path resolve(String relativePath) {
    if (root == null) {
      throw new BadRequestException(ErrorCodes.Vault.NOT_CONFIGURED);
    }
    if (relativePath == null || relativePath.isBlank()) {
      throw invalid();
    }
    Path relative = Path.of(relativePath.strip());
    if (relative.isAbsolute()) {
      throw invalid();
    }
    for (Path part : relative) {
      if (part.toString().equals("..")) {
        throw invalid();
      }
    }
    try {
      // toRealPath segue links simbólicos: um link que aponte para fora da raiz cai no teste abaixo.
      Path candidate = root.resolve(relative).toRealPath();
      if (!candidate.startsWith(root) || !Files.isDirectory(candidate)) {
        throw invalid();
      }
      return candidate;
    } catch (IOException missing) {
      throw invalid();
    }
  }

  public Optional<Path> tryResolve(String relativePath) {
    try {
      return Optional.of(resolve(relativePath));
    } catch (BadRequestException invalid) {
      return Optional.empty();
    }
  }

  private static BadRequestException invalid() {
    return new BadRequestException(ErrorCodes.Vault.INVALID_PATH);
  }
}
