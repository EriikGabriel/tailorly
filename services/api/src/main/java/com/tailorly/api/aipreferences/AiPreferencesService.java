package com.tailorly.api.aipreferences;

import java.util.UUID;

import com.tailorly.api.aipreferences.dto.AiPreferencesResponse;
import com.tailorly.api.aipreferences.dto.UpdateAiPreferencesRequest;
import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
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
  private final String defaultProvider;

  public AiPreferencesService(AiPreferencesRepository repository, OwnerRepository ownerRepository,
      SecretEncryptor secretEncryptor, RemoteInterpretationProperties remoteDefaults,
      OllamaInterpretationProperties ollamaDefaults,
      @Value("${tailorly.interpretation.provider:rules}") String defaultProvider) {
    this.repository = repository;
    this.ownerRepository = ownerRepository;
    this.secretEncryptor = secretEncryptor;
    this.remoteDefaults = remoteDefaults;
    this.ollamaDefaults = ollamaDefaults;
    this.defaultProvider = defaultProvider;
  }

  @Transactional(readOnly = true)
  public AiPreferencesResponse find(UUID ownerId) {
    return repository.findByOwnerId(ownerId)
        .map(AiPreferencesResponse::from)
        .orElseGet(() -> AiPreferencesResponse.serverDefaults(defaultProvider, remoteDefaults, ollamaDefaults));
  }

  @Transactional
  public AiPreferencesResponse save(UUID ownerId, UpdateAiPreferencesRequest request) {
    AiPreferences preferences = repository.findByOwnerId(ownerId)
        .orElseGet(() -> AiPreferences.builder().owner(ownerRepository.getReferenceById(ownerId)).build());

    preferences.setProvider(request.provider());
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

    return AiPreferencesResponse.from(repository.save(preferences));
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
        orDefault(secretEncryptor.decrypt(preferences.getOllamaApiKeyEncrypted()), ollamaDefaults.apiKey()));
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
