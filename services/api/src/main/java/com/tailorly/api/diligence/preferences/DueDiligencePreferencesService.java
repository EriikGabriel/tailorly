package com.tailorly.api.diligence.preferences;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preferência da conta sobre a etapa automática de due diligence (consulta à empresa disparada pela
 * leitura de uma vaga). Ligada por padrão, inclusive sem linha gravada. Só contas têm a preferência.
 */
@Service
@RequiredArgsConstructor
public class DueDiligencePreferencesService {
  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final JdbcTemplate jdbc;

  @Transactional(readOnly = true)
  public DueDiligencePreferencesResponse get(UUID ownerId) {
    account(ownerId);
    return new DueDiligencePreferencesResponse(isPipelineEnabled(ownerId));
  }

  @Transactional
  public DueDiligencePreferencesResponse update(UUID ownerId, UpdateDueDiligencePreferencesRequest request) {
    account(ownerId);
    boolean enabled = request.pipelineEnabled() != null ? request.pipelineEnabled() : isPipelineEnabled(ownerId);
    jdbc.update("insert into owner_due_diligence_preferences (owner_id, pipeline_enabled, updated_at) "
        + "values (?, ?, ?) on conflict (owner_id) do update set pipeline_enabled = excluded.pipeline_enabled, "
        + "updated_at = excluded.updated_at", ownerId, enabled, Timestamp.from(Instant.now()));
    return new DueDiligencePreferencesResponse(enabled);
  }

  /** Sem linha gravada, a etapa automática está ligada. Quem chama já verificou o acesso ao owner. */
  @Transactional(readOnly = true)
  public boolean isPipelineEnabled(UUID ownerId) {
    return jdbc.query("select pipeline_enabled from owner_due_diligence_preferences where owner_id = ?",
        (rs, rowNum) -> rs.getBoolean(1), ownerId).stream().findFirst().orElse(true);
  }

  private Owner account(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    if (AnonymousQuotaService.isAnonymous(owner)) {
      throw new BadRequestException(ErrorCodes.Diligence.PREFERENCES_ACCOUNT_ONLY);
    }
    return owner;
  }
}
