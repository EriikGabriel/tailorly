package com.tailorly.api.vault;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.search.WebSearchClient;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.vault.dto.UpdateOwnerVaultRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preferências de segundo cérebro de cada conta: a pasta do vault Obsidian (relativa à raiz do
 * servidor), se o vault é lido, se os currículos são exportados para ele e se a busca web é usada.
 * Só contas têm essas preferências; sessões anônimas não. Quem chama os métodos de leitura interna
 * ({@link #readableVaultOf}, {@link #exportTargetOf}, {@link #webSearchEnabledFor}) já deve ter
 * verificado o acesso ao owner.
 */
@Service
@RequiredArgsConstructor
public class OwnerVaultService {
  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final VaultLocations locations;
  private final VaultExportProperties exportProperties;
  private final WebSearchClient webSearch;
  private final JdbcTemplate jdbc;

  /**
   * @param rootAvailable      se o servidor tem uma raiz de vaults configurada
   * @param webSearchAvailable se o servidor tem um SearXNG configurado
   * @param relativePath       pasta do vault escolhida pela conta, ou nulo
   * @param usable             se a pasta ainda existe e está dentro da raiz
   * @param exportSubfolder    subpasta de exportação escolhida, ou nulo (usa o padrão do servidor)
   */
  public record Settings(boolean rootAvailable, boolean webSearchAvailable, String relativePath, boolean usable,
      boolean readEnabled, boolean exportEnabled, String exportSubfolder, boolean webSearchEnabled) {
  }

  private record Row(String relativePath, boolean readEnabled, boolean exportEnabled, String exportSubfolder,
      boolean webSearchEnabled) {
    static final Row DEFAULT = new Row(null, false, false, null, true);
  }

  @Transactional(readOnly = true)
  public Settings get(UUID ownerId) {
    account(ownerId);
    return settings(row(ownerId));
  }

  /** Substitui as preferências da conta pelas enviadas; o caminho é validado dentro da raiz. */
  @Transactional
  public Settings set(UUID ownerId, UpdateOwnerVaultRequest request) {
    account(ownerId);
    String path = blankToNull(request.relativePath());
    String subfolder = blankToNull(request.exportSubfolder());
    if (path != null) {
      locations.resolve(path);
    } else if (request.readEnabled() || request.exportEnabled()) {
      throw new BadRequestException(ErrorCodes.Vault.PATH_REQUIRED);
    }
    if (subfolder != null) {
      validateSubfolder(subfolder);
    }
    Row row = new Row(path == null ? null : Path.of(path).normalize().toString(), request.readEnabled(),
        request.exportEnabled(), subfolder, request.webSearchEnabled());
    jdbc.update("insert into owner_vaults (owner_id, relative_path, updated_at, read_enabled, export_enabled, "
        + "export_subfolder, web_search_enabled) values (?, ?, ?, ?, ?, ?, ?) "
        + "on conflict (owner_id) do update set relative_path = excluded.relative_path, "
        + "updated_at = excluded.updated_at, read_enabled = excluded.read_enabled, "
        + "export_enabled = excluded.export_enabled, export_subfolder = excluded.export_subfolder, "
        + "web_search_enabled = excluded.web_search_enabled",
        ownerId, row.relativePath(), Timestamp.from(Instant.now()), row.readEnabled(), row.exportEnabled(),
        row.exportSubfolder(), row.webSearchEnabled());
    return settings(row);
  }

  /** Remove o vault e desliga leitura e exportação; a preferência de busca web é mantida. */
  @Transactional
  public void clear(UUID ownerId) {
    account(ownerId);
    jdbc.update("update owner_vaults set relative_path = null, read_enabled = false, export_enabled = false, "
        + "export_subfolder = null, updated_at = ? where owner_id = ?", Timestamp.from(Instant.now()), ownerId);
  }

  /** Pasta do vault para leitura, se a conta ligou a leitura e o caminho ainda é válido. */
  @Transactional(readOnly = true)
  public Optional<Path> readableVaultOf(UUID ownerId) {
    Row row = row(ownerId);
    return row.readEnabled() && row.relativePath() != null ? locations.tryResolve(row.relativePath()) : Optional.empty();
  }

  /** Pasta de destino da exportação (vault + subpasta), se a conta ligou a exportação e o vault é válido. */
  @Transactional(readOnly = true)
  public Optional<Path> exportTargetOf(UUID ownerId) {
    Row row = row(ownerId);
    if (!row.exportEnabled() || row.relativePath() == null) {
      return Optional.empty();
    }
    String subfolder = row.exportSubfolder() != null ? row.exportSubfolder() : exportProperties.subfolder();
    return locations.tryResolve(row.relativePath()).map(vault -> vault.resolve(subfolder));
  }

  @Transactional(readOnly = true)
  public boolean webSearchEnabledFor(UUID ownerId) {
    return row(ownerId).webSearchEnabled();
  }

  private Settings settings(Row row) {
    boolean usable = row.relativePath() != null && locations.tryResolve(row.relativePath()).isPresent();
    return new Settings(locations.isAvailable(), webSearch.isConfigured(), row.relativePath(), usable,
        row.readEnabled(), row.exportEnabled(), row.exportSubfolder(), row.webSearchEnabled());
  }

  private Row row(UUID ownerId) {
    return jdbc.query("select relative_path, read_enabled, export_enabled, export_subfolder, web_search_enabled "
        + "from owner_vaults where owner_id = ?",
        (rs, rowNum) -> new Row(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3), rs.getString(4),
            rs.getBoolean(5)), ownerId).stream().findFirst().orElse(Row.DEFAULT);
  }

  private static void validateSubfolder(String subfolder) {
    Path relative = Path.of(subfolder);
    if (relative.isAbsolute()) {
      throw new BadRequestException(ErrorCodes.Vault.SUBFOLDER_INVALID);
    }
    for (Path part : relative) {
      if (part.toString().equals("..")) {
        throw new BadRequestException(ErrorCodes.Vault.SUBFOLDER_INVALID);
      }
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private Owner account(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    if (AnonymousQuotaService.isAnonymous(owner)) {
      throw new BadRequestException(ErrorCodes.Vault.ACCOUNT_ONLY);
    }
    return owner;
  }
}
