package com.tailorly.api.aipreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.security.SecretEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiPreferencesServiceAnonymousTest {
  @Mock AiPreferencesRepository repository;
  @Mock OwnerRepository ownerRepository;
  @Mock SecretEncryptor secretEncryptor;

  private final UUID ownerId = UUID.randomUUID();

  private AiPreferencesService service() {
    return new AiPreferencesService(repository, ownerRepository, secretEncryptor,
        new RemoteInterpretationProperties("https://env.example/v1/chat/completions", "env-model", "env-key"),
        new OllamaInterpretationProperties("http://127.0.0.1:11434/v1/chat/completions", "env-ollama", "ollama"),
        "remote-with-ollama-fallback");
  }

  @Test
  void anonymousOwnersAlwaysUseTheServerEnvironmentConfiguration() {
    Owner anonymous = new Owner();
    anonymous.setId(ownerId);
    anonymous.setKind(OwnerKindEnum.KIND_ANONYMOUS);
    when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(anonymous));

    EffectiveAiSettings settings = service().resolveEffectiveSettings(ownerId);

    assertEquals("remote-with-ollama-fallback", settings.provider());
    assertEquals("https://env.example/v1/chat/completions", settings.remoteEndpoint());
    assertEquals("env-model", settings.remoteModel());
    assertEquals("env-key", settings.remoteApiKey());
    assertEquals("env-ollama", settings.ollamaModel());
    // Nem a preferência salva (que um visitante não deveria ter) é consultada.
    verifyNoInteractions(repository);
  }
}
