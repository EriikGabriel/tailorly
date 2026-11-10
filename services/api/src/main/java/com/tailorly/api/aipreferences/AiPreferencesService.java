package com.tailorly.api.aipreferences;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.tailorly.api.ai.ClaudeCliProperties;
import com.tailorly.api.ai.CodexCliProperties;
import com.tailorly.api.ai.LocalCliEfforts;
import com.tailorly.api.aipreferences.dto.AiPreferencesResponse;
import com.tailorly.api.aipreferences.dto.UpdateAiPreferencesRequest;
import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.security.SecretEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lê e grava a preferência de IA de um owner e resolve a configuração efetiva usada pela
 * extração: a preferência salva pelo owner, com fallback campo a campo para a configuração
 * global do servidor ({@code tailorly.interpretation.*}) onde o owner não configurou nada.
 *
 * <p>Um owner sem {@link AiPreferences} nunca salvou preferência própria e usa inteiramente a
 * configuração global — comportamento idêntico ao de antes desta funcionalidade existir.</p>
 */
@Service
@EnableConfigurationProperties({RemoteInterpretationProperties.class, OllamaInterpretationProperties.class})
public class AiPreferencesService {
  private final AiPreferencesRepository repository;
  private final OwnerRepository ownerRepository;
  private final SecretEncryptor secretEncryptor;
  private final RemoteInterpretationProperties remoteDefaults;
  private final OllamaInterpretationProperties ollamaDefaults;
  private final ClaudeCliProperties claudeCliDefaults;
  private final CodexCliProperties codexCliDefaults;
  private final String defaultProvider;

  public AiPreferencesService(AiPreferencesRepository repository, OwnerRepository ownerRepository,
      SecretEncryptor secretEncryptor, RemoteInterpretationProperties remoteDefaults,
      OllamaInterpretationProperties ollamaDefaults, ClaudeCliProperties claudeCliDefaults,
      CodexCliProperties codexCliDefaults,
      @Value("${tailorly.interpretation.provider:rules}") String defaultProvider) {
    this.repository = repository;
    this.ownerRepository = ownerRepository;
    this.secretEncryptor = secretEncryptor;
    this.remoteDefaults = remoteDefaults;
    this.ollamaDefaults = ollamaDefaults;
    this.claudeCliDefaults = claudeCliDefaults;
    this.codexCliDefaults = codexCliDefaults;
    this.defaultProvider = defaultProvider;
  }

  @Transactional(readOnly = true)
  public AiPreferencesResponse find(UUID ownerId) {
    return repository.findByOwnerId(ownerId)
        .map(preferences -> AiPreferencesResponse.from(preferences, claudeCliDefaults, codexCliDefaults))
        .orElseGet(() -> AiPreferencesResponse.serverDefaults(defaultProvider, remoteDefaults, ollamaDefaults,
            claudeCliDefaults, codexCliDefaults));
  }

  @Transactional
  public AiPreferencesResponse save(UUID ownerId, UpdateAiPreferencesRequest request) {
    AiPreferences preferences = repository.findByOwnerId(ownerId)
        .orElseGet(() -> AiPreferences.builder().owner(ownerRepository.getReferenceById(ownerId)).build());

    // claude-cli/codex-cli rodam o CLI logado na máquina do servidor: só existem onde o servidor os habilitou
    // (uso local). O executável nunca vem da requisição.
    if ("claude-cli".equals(request.provider()) && !claudeCliDefaults.enabled())
      throw new BadRequestException(ErrorCodes.AiPreferences.CLAUDE_CLI_UNAVAILABLE);
    if ("codex-cli".equals(request.provider()) && !codexCliDefaults.enabled())
      throw new BadRequestException(ErrorCodes.AiPreferences.CODEX_CLI_UNAVAILABLE);

    preferences.setProvider(request.provider());
    // Sem o bloco na requisição, mantém o que já estava salvo para aquele CLI.
    if (request.claudeCli() != null) {
      preferences.setClaudeCliModel(blankToNull(request.claudeCli().model()));
      preferences.setClaudeCliTimeoutSeconds(request.claudeCli().timeoutSeconds());
      preferences.setClaudeCliEfforts(validEfforts(request.claudeCli().efforts()));
    }
    if (request.codexCli() != null) {
      preferences.setCodexCliModel(blankToNull(request.codexCli().model()));
      preferences.setCodexCliTimeoutSeconds(request.codexCli().timeoutSeconds());
      preferences.setCodexCliEfforts(validEfforts(request.codexCli().efforts()));
    }
    preferences.setRemoteEndpoint(blankToNull(request.remoteEndpoint()));
    preferences.setRemoteModel(blankToNull(request.remoteModel()));
    preferences.setOllamaEndpoint(blankToNull(request.ollamaEndpoint()));
    preferences.setOllamaModel(blankToNull(request.ollamaModel()));

    // null = mantém a chave já guardada; uma string (mesmo vazia) substitui/limpa o valor.
    if (request.remoteApiKey() != null) {
      preferences.setRemoteApiKeyEncrypted(secretEncryptor.encrypt(request.remoteApiKey()));
    }
    if (request.ollamaApiKey() != null) {
      preferences.setOllamaApiKeyEncrypted(secretEncryptor.encrypt(request.ollamaApiKey()));
    }

    return AiPreferencesResponse.from(repository.save(preferences), claudeCliDefaults, codexCliDefaults);
  }

  /** Usado pela extração (veja {@code com.tailorly.api.extraction.interpreting}). */
  @Transactional(readOnly = true)
  public EffectiveAiSettings resolveEffectiveSettings(UUID ownerId) {
    // Visitantes sempre usam a IA definida nas variáveis de ambiente do servidor.
    if (isAnonymous(ownerId)) {
      return defaultEffectiveSettings();
    }
    return repository.findByOwnerId(ownerId)
        .map(this::toEffectiveSettings)
        .orElseGet(this::defaultEffectiveSettings);
  }

  private boolean isAnonymous(UUID ownerId) {
    return ownerRepository.findById(ownerId)
        .map(owner -> owner.getKind() == OwnerKindEnum.KIND_ANONYMOUS)
        .orElse(false);
  }

  private EffectiveAiSettings toEffectiveSettings(AiPreferences preferences) {
    return new EffectiveAiSettings(
        preferences.getProvider(),
        orDefault(preferences.getRemoteEndpoint(), remoteDefaults.endpoint()),
        orDefault(preferences.getRemoteModel(), remoteDefaults.model()),
        orDefault(secretEncryptor.decrypt(preferences.getRemoteApiKeyEncrypted()), remoteDefaults.apiKey()),
        orDefault(preferences.getOllamaEndpoint(), ollamaDefaults.endpoint()),
        orDefault(preferences.getOllamaModel(), ollamaDefaults.model()),
        orDefault(secretEncryptor.decrypt(preferences.getOllamaApiKeyEncrypted()), ollamaDefaults.apiKey()),
        new EffectiveAiSettings.LocalCliOverrides(
            preferences.getClaudeCliModel(), seconds(preferences.getClaudeCliTimeoutSeconds()),
            preferences.getClaudeCliEfforts()),
        new EffectiveAiSettings.LocalCliOverrides(
            preferences.getCodexCliModel(), seconds(preferences.getCodexCliTimeoutSeconds()),
            preferences.getCodexCliEfforts()));
  }

  private static Duration seconds(Integer value) {
    return value == null ? null : Duration.ofSeconds(value);
  }

  /** Só {@code default} e etapas conhecidas, com níveis válidos; vazio vira nulo (usa o servidor). */
  private static Map<String, String> validEfforts(Map<String, String> requested) {
    if (requested == null || requested.isEmpty())
      return null;
    Map<String, String> efforts = new LinkedHashMap<>();
    requested.forEach((key, value) -> {
      boolean knownKey = LocalCliEfforts.DEFAULT_KEY.equals(key) || LocalCliEfforts.TASKS.contains(key);
      String level = LocalCliEfforts.normalize(value);
      if (!knownKey || level == null)
        throw new BadRequestException(ErrorCodes.AiPreferences.INVALID_EFFORT);
      efforts.put(key, level);
    });
    return efforts;
  }

  private EffectiveAiSettings defaultEffectiveSettings() {
    return new EffectiveAiSettings(defaultProvider, remoteDefaults.endpoint(), remoteDefaults.model(),
        remoteDefaults.apiKey(), ollamaDefaults.endpoint(), ollamaDefaults.model(), ollamaDefaults.apiKey());
  }

  private static String orDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
