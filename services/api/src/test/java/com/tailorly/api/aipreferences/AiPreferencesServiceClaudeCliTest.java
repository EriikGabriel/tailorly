package com.tailorly.api.aipreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.ai.ClaudeCliProperties;
import com.tailorly.api.ai.CodexCliProperties;
import com.tailorly.api.ai.LocalCliEfforts;
import com.tailorly.api.aipreferences.dto.AiPreferencesResponse;
import com.tailorly.api.aipreferences.dto.LocalCliPreferencesRequest;
import com.tailorly.api.aipreferences.dto.UpdateAiPreferencesRequest;
import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.security.SecretEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiPreferencesServiceClaudeCliTest {
  @Mock AiPreferencesRepository repository;
  @Mock OwnerRepository ownerRepository;
  @Mock SecretEncryptor secretEncryptor;

  private final UUID ownerId = UUID.randomUUID();

  private AiPreferencesService service(boolean cliEnabled) {
    return new AiPreferencesService(repository, ownerRepository, secretEncryptor,
        new RemoteInterpretationProperties("https://env.example/v1/chat/completions", "env-model", "env-key"),
        new OllamaInterpretationProperties("http://127.0.0.1:11434/v1/chat/completions", "env-ollama", "ollama"),
        new ClaudeCliProperties(cliEnabled, "claude", "sonnet", Duration.ofMinutes(3), "medium", Map.of()),
        new CodexCliProperties(cliEnabled, "codex", "", Duration.ofMinutes(2), "medium", Map.of()),
        "rules");
  }

  private static UpdateAiPreferencesRequest request(String provider, Map<String, String> efforts) {
    LocalCliPreferencesRequest cli = new LocalCliPreferencesRequest("opus", 300, efforts);
    return new UpdateAiPreferencesRequest(provider, null, null, null, null, null, null,
        "claude-cli".equals(provider) ? cli : null, "codex-cli".equals(provider) ? cli : null);
  }

  private void savesWhatItReceives() {
    when(repository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
    when(ownerRepository.getReferenceById(ownerId)).thenReturn(new Owner());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void refusesClaudeCliWhenTheServerDidNotEnableIt() {
    savesWhatItReceives();

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service(false).save(ownerId, request("claude-cli", null)));
    assertEquals(ErrorCodes.AiPreferences.CLAUDE_CLI_UNAVAILABLE, error.getMessage());
  }

  @Test
  void refusesCodexCliWhenTheServerDidNotEnableIt() {
    savesWhatItReceives();

    BadRequestException error = assertThrows(BadRequestException.class,
        () -> service(false).save(ownerId, request("codex-cli", null)));
    assertEquals(ErrorCodes.AiPreferences.CODEX_CLI_UNAVAILABLE, error.getMessage());
  }

  @Test
  void savesCodexChoicesIndependentlyOfClaude() {
    savesWhatItReceives();

    AiPreferencesResponse response = service(true)
        .save(ownerId, request("codex-cli", Map.of("interpretation", "max")));

    assertEquals("opus", response.codexCli().model());
    assertEquals("max", response.codexCli().efforts().get("interpretation"));
    assertEquals("sonnet", response.claudeCli().model());
  }

  @Test
  void rejectsUnknownStepsAndLevels() {
    savesWhatItReceives();

    assertThrows(BadRequestException.class,
        () -> service(true).save(ownerId, request("claude-cli", Map.of("unknown-step", "high"))));
    assertThrows(BadRequestException.class,
        () -> service(true).save(ownerId, request("claude-cli", Map.of("interpretation", "turbo"))));
  }

  @Test
  void savesAccountChoicesAndReturnsThemOverServerDefaults() {
    savesWhatItReceives();

    AiPreferencesResponse response = service(true)
        .save(ownerId, request("claude-cli", Map.of("job-analysis", "XHIGH", "default", "low")));

    assertTrue(response.claudeCli().available());
    assertEquals("opus", response.claudeCli().model());
    assertEquals(300, response.claudeCli().timeoutSeconds());
    assertEquals("xhigh", response.claudeCli().efforts().get("job-analysis"));
    assertEquals("high", response.claudeCli().efforts().get("interpretation"));
    assertEquals("low", response.claudeCli().efforts().get("default"));
    assertEquals("medium", response.claudeCli().serverEfforts().get("job-analysis"));
    assertEquals("sonnet", response.claudeCli().serverModel());
    assertEquals(LocalCliEfforts.TASKS, response.localCliTasks());
    // O bloco do Codex não veio na requisição: continua com os padrões do servidor.
    assertEquals("", response.codexCli().model());
    assertEquals(120, response.codexCli().timeoutSeconds());
  }

  @Test
  void effectiveSettingsCarryTheAccountOverrides() {
    Owner account = new Owner();
    account.setId(ownerId);
    account.setKind(OwnerKindEnum.KIND_ACCOUNT);
    when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(account));
    AiPreferences saved = AiPreferences.builder().provider("claude-cli").claudeCliModel("opus")
        .claudeCliTimeoutSeconds(300).claudeCliEfforts(Map.of("job-analysis", "xhigh"))
        .codexCliModel("gpt-x").codexCliEfforts(Map.of("interpretation", "low")).build();
    when(repository.findByOwnerId(ownerId)).thenReturn(Optional.of(saved));

    EffectiveAiSettings settings = service(true).resolveEffectiveSettings(ownerId);

    assertEquals("claude-cli", settings.provider());
    assertEquals("opus", settings.claudeCli().model());
    assertEquals(Duration.ofSeconds(300), settings.claudeCli().timeout());
    assertEquals("xhigh", settings.claudeCli().efforts().get("job-analysis"));
    assertEquals("gpt-x", settings.codexCli().model());
    assertEquals("low", settings.codexCli().efforts().get("interpretation"));
  }

  @Test
  void serverDefaultsWhenTheAccountNeverSaved() {
    when(repository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

    AiPreferencesResponse response = service(false).find(ownerId);

    assertFalse(response.claudeCli().available());
    assertFalse(response.codexCli().available());
    assertEquals("sonnet", response.claudeCli().model());
    assertEquals(180, response.claudeCli().timeoutSeconds());
    assertEquals("low", response.claudeCli().efforts().get("resume-layout-plan"));
    assertEquals("low", response.codexCli().efforts().get("resume-layout-plan"));
    assertNull(response.updatedAt());
  }
}
