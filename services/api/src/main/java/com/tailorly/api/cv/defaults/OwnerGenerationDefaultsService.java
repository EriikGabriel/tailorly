package com.tailorly.api.cv.defaults;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.cv.template.CvTemplateCatalog;
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
 * Padrões de geração de uma conta: o layout de currículo escolhido. Só contas guardam padrões; a
 * sessão anônima escolhe no próprio navegador e não deixa histórico no servidor.
 */
@Service
@RequiredArgsConstructor
public class OwnerGenerationDefaultsService {
  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final JdbcTemplate jdbc;

  /** Nome do template padrão da conta, ou nulo quando ela ainda não escolheu. */
  @Transactional(readOnly = true)
  public String templateName(UUID ownerId) {
    account(ownerId);
    return jdbc.queryForList("select template_name from owner_generation_defaults where owner_id = ?",
        String.class, ownerId).stream().findFirst().orElse(null);
  }

  @Transactional
  public String setTemplateName(UUID ownerId, String templateName) {
    account(ownerId);
    if (!CvTemplateCatalog.exists(templateName)) {
      throw new BadRequestException(ErrorCodes.CvGeneration.TEMPLATE_UNKNOWN);
    }
    jdbc.update("insert into owner_generation_defaults (owner_id, template_name, updated_at) values (?, ?, ?) "
        + "on conflict (owner_id) do update set template_name = excluded.template_name, "
        + "updated_at = excluded.updated_at", ownerId, templateName, Timestamp.from(Instant.now()));
    return templateName;
  }

  private Owner account(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    if (AnonymousQuotaService.isAnonymous(owner)) {
      throw new BadRequestException(ErrorCodes.CvGeneration.DEFAULTS_ACCOUNT_ONLY);
    }
    return owner;
  }
}
