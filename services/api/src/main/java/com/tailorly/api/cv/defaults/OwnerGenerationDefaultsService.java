package com.tailorly.api.cv.defaults;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.defaults.dto.GenerationDefaultsResponse;
import com.tailorly.api.cv.defaults.dto.UpdateGenerationDefaultsRequest;
import com.tailorly.api.cv.model.CvLanguage;
import com.tailorly.api.cv.model.CvPreferences;

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
 * Padrões de geração de uma conta: layout de currículo, otimização para ATS e diretrizes de destaque.
 * São o ponto de partida de cada geração; o gerador ajusta diretrizes e layout por vaga sem gravar
 * aqui. Só contas guardam padrões; a sessão anônima escolhe no próprio navegador e não deixa
 * histórico no servidor.
 */
@Service
@RequiredArgsConstructor
public class OwnerGenerationDefaultsService {
  /** Mesmo conjunto que a interface liga por padrão. */
  static final List<String> DEFAULT_GUIDELINES = List.of("Destacar Liderança Técnica",
      "Arquitetura em Nuvem & Escala", "Impacto de Negócio Quantificado");
  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
  };

  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  /** Padrões da conta; sem linha gravada, o layout é nulo, o ATS fica ligado e valem as diretrizes padrão. */
  @Transactional(readOnly = true)
  public GenerationDefaultsResponse get(UUID ownerId) {
    account(ownerId);
    return row(ownerId);
  }

  /** Aplica só os campos enviados e devolve os padrões resultantes. */
  @Transactional
  public GenerationDefaultsResponse update(UUID ownerId, UpdateGenerationDefaultsRequest request) {
    account(ownerId);
    if (request.templateName() != null && !CvTemplateCatalog.exists(request.templateName())) {
      throw new BadRequestException(ErrorCodes.CvGeneration.TEMPLATE_UNKNOWN);
    }
    GenerationDefaultsResponse current = row(ownerId);
    GenerationDefaultsResponse next = new GenerationDefaultsResponse(
        request.templateName() != null ? request.templateName() : current.templateName(),
        request.optimizeForAts() != null ? request.optimizeForAts() : current.optimizeForAts(),
        request.guidelines() != null ? request.guidelines().stream().distinct().toList() : current.guidelines(),
        request.language() != null ? CvLanguage.normalizePreference(request.language()) : current.language());
    jdbc.update("insert into owner_generation_defaults (owner_id, template_name, optimize_for_ats, "
        + "guidelines, language, updated_at) values (?, ?, ?, ?, ?, ?) "
        + "on conflict (owner_id) do update set template_name = excluded.template_name, "
        + "optimize_for_ats = excluded.optimize_for_ats, guidelines = excluded.guidelines, "
        + "language = excluded.language, updated_at = excluded.updated_at", ownerId, next.templateName(),
        next.optimizeForAts(), write(next.guidelines()), next.language(), Timestamp.from(Instant.now()));
    return next;
  }

  /**
   * Preferências usadas quando a requisição de geração não traz as suas. Não verifica acesso (quem
   * gera já verificou); sem padrões gravados, como em sessões anônimas, valem os padrões do servidor.
   */
  @Transactional(readOnly = true)
  public CvPreferences preferencesFor(UUID ownerId) {
    GenerationDefaultsResponse defaults = row(ownerId);
    return new CvPreferences(defaults.guidelines(), defaults.optimizeForAts(), defaults.templateName(),
        defaults.language());
  }

  private GenerationDefaultsResponse row(UUID ownerId) {
    return jdbc.query("select template_name, optimize_for_ats, guidelines, language "
        + "from owner_generation_defaults where owner_id = ?",
        (rs, rowNum) -> new GenerationDefaultsResponse(rs.getString(1), rs.getBoolean(2), read(rs.getString(3)),
            rs.getString(4)),
        ownerId).stream().findFirst().orElse(new GenerationDefaultsResponse(null, true, DEFAULT_GUIDELINES,
            CvLanguage.AUTO));
  }

  private List<String> read(String json) {
    try {
      return objectMapper.readValue(json, STRINGS);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Invalid stored guidelines", exception);
    }
  }

  private String write(List<String> guidelines) {
    try {
      return objectMapper.writeValueAsString(guidelines);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Could not store guidelines", exception);
    }
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
