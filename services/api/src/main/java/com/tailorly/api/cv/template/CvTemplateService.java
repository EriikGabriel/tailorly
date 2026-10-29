package com.tailorly.api.cv.template;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import com.tailorly.api.cv.dto.CvTemplateResponse;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Templates Typst que uma pessoa pode editar. O embarcado nunca muda; editar cria uma nova versão
 * imutável de {@code custom} no owner, e a mais recente passa a ser a usada nas próximas gerações.
 */
@Service
@RequiredArgsConstructor
public class CvTemplateService {
  public static final String CUSTOM_NAME = "custom";
  private static final int MAX_ANONYMOUS_VERSIONS = 20;
  // Pacotes são baixados da rede pelo compilador; o template só pode usar o que está no próprio arquivo.
  private static final Pattern PACKAGE_IMPORT = Pattern.compile("@(preview|local)\\s*/");

  private final CvTemplateCatalog catalog;
  private final CvTemplateRepository templates;
  private final OwnerRepository owners;
  private final OwnerAccessService access;

  /** Recusa código que o compilador resolveria fora do arquivo (pacotes remotos). */
  public static void validateSource(String content) {
    if (content == null || content.isBlank() || content.length() > 64_000 || PACKAGE_IMPORT.matcher(content).find()) {
      throw new BadRequestException(ErrorCodes.CvGeneration.TEMPLATE_INVALID);
    }
  }

  // Não é somente leitura: a primeira consulta registra o template embarcado.
  @Transactional
  public CvTemplateResponse bundled() {
    return CvTemplateResponse.from(catalog.latest(CvTemplateCatalog.DEFAULT_NAME));
  }

  @Transactional
  public CvTemplateResponse current(UUID ownerId) {
    Owner owner = owner(ownerId);
    access.assertCanAccess(owner);
    return CvTemplateResponse.from(resolveCurrent(owner));
  }

  @Transactional(readOnly = true)
  public CvTemplateResponse findById(UUID id) {
    CvTemplate template = templates.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.TEMPLATE_NOT_FOUND));
    if (template.getOwner() != null) {
      access.assertCanAccess(template.getOwner());
    }
    return CvTemplateResponse.from(template);
  }

  @Transactional
  public CvTemplateResponse save(UUID ownerId, String content) {
    validateSource(content);
    Owner owner = owners.findByIdForUpdate(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);

    String hash = sha256(content);
    var latest = templates.findFirstByOwnerIdAndNameOrderByVersionDesc(ownerId, CUSTOM_NAME);
    if (latest.isPresent() && latest.get().getContentHash().equals(hash)) {
      return CvTemplateResponse.from(latest.get());
    }
    if (AnonymousQuotaService.isAnonymous(owner) && templates.countByOwnerId(ownerId) >= MAX_ANONYMOUS_VERSIONS) {
      throw new TooManyRequestsException(ErrorCodes.CvGeneration.TEMPLATE_LIMIT_REACHED);
    }
    return CvTemplateResponse.from(templates.save(CvTemplate.builder()
        .owner(owner)
        .name(CUSTOM_NAME)
        .version(latest.map(template -> template.getVersion() + 1).orElse(1))
        .content(content)
        .contentHash(hash)
        .createdAt(Instant.now())
        .build()));
  }

  /** O template que o owner usa por padrão: a versão editada mais recente ou o embarcado. */
  public CvTemplate resolveCurrent(Owner owner) {
    return templates.findFirstByOwnerIdAndNameOrderByVersionDesc(owner.getId(), CUSTOM_NAME)
        .orElseGet(() -> catalog.latest(CvTemplateCatalog.DEFAULT_NAME));
  }

  /** Um template pedido explicitamente: embarcado ou do próprio owner; outro caso é indistinguível de inexistente. */
  public CvTemplate resolveFor(Owner owner, UUID templateId) {
    if (templateId == null) {
      return resolveCurrent(owner);
    }
    CvTemplate template = templates.findById(templateId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.TEMPLATE_NOT_FOUND));
    if (template.getOwner() != null && !template.getOwner().getId().equals(owner.getId())) {
      throw new ResourceNotFoundException(ErrorCodes.CvGeneration.TEMPLATE_NOT_FOUND);
    }
    return template;
  }

  public CvTemplate reference(UUID id) {
    return templates.getReferenceById(id);
  }

  private Owner owner(UUID id) {
    return owners.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
  }

  private static String sha256(String content) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
