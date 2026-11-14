package com.tailorly.api.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Executa {@code claude -p} (Claude Code em modo não interativo) com a conta já logada no CLI. O
 * consumo entra nos limites da assinatura dessa pessoa, por isso o provider é só para uso local e
 * fica desligado por padrão ({@code TAILORLY_CLAUDE_CLI_ENABLED}).
 *
 * <p>Sem ferramentas, MCP, configurações nem sessão salva: o CLI só responde ao prompt. Roda em um
 * diretório temporário para não carregar CLAUDE.md/AGENTS.md do projeto, remove do ambiente chaves,
 * base URL e marcadores de sessão para não cobrar a API em vez da assinatura e executa um processo por vez.
 */
@Component
@EnableConfigurationProperties(ClaudeCliProperties.class)
public class ClaudeCliClient {
  private static final Logger log = LoggerFactory.getLogger(ClaudeCliClient.class);
  private static final String LABEL = "Claude Code CLI";
  private static final int MAX_ERROR_CHARS = 500;
  // Equivalente ao response_format json_object das APIs Chat Completions, que o CLI não tem.
  private static final String JSON_ONLY =
      "\n\nResponda somente com o JSON pedido: sem texto antes ou depois e sem cercas de markdown.";

  private final ClaudeCliProperties properties;
  private final ObjectMapper objectMapper;
  private final Semaphore oneAtATime = new Semaphore(1, true);

  public ClaudeCliClient(ClaudeCliProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public boolean enabled() {
    return properties.enabled();
  }

  public String model() {
    return properties.model();
  }

  public ClaudeCliProperties properties() {
    return properties;
  }

  /** Esforço da etapa, conforme {@link LocalCliProperties#effortFor(String, Map)}. */
  public String effortFor(String task, Map<String, String> accountEfforts) {
    return properties.effortFor(task, accountEfforts);
  }

  /** Envia o prompt e devolve o texto do campo {@code result}; a validação fica com quem chama. */
  public String complete(String model, String systemPrompt, String userPrompt) {
    return complete(model, null, null, systemPrompt, userPrompt);
  }

  /** {@code effort} nulo ou inválido e {@code timeout} nulo ou fora dos limites usam o servidor. */
  public String complete(String model, String effort, Duration timeout, String systemPrompt, String userPrompt) {
    if (!properties.enabled())
      throw new IllegalStateException(LABEL + " provider is disabled (set TAILORLY_CLAUDE_CLI_ENABLED=true for local use)");

    try {
      oneAtATime.acquire();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the " + LABEL, interrupted);
    }

    Path workDir = null;
    try {
      workDir = LocalCliProcess.createWorkDir("tailorly-claude-cli-");
      Path systemPromptFile = workDir.resolve("system-prompt.txt");
      Files.writeString(systemPromptFile, (systemPrompt == null ? "" : systemPrompt) + JSON_ONLY, StandardCharsets.UTF_8);
      String level = LocalCliEfforts.normalize(effort) != null ? LocalCliEfforts.normalize(effort) : properties.effort();
      String chosenModel = model == null || model.isBlank() ? properties.model() : model;

      // ANTHROPIC_* (chave, token, base URL, remapeamento de aliases) faria o CLI cobrar a API ou ir para
      // outro endpoint em vez de usar a assinatura logada; CLAUDECODE/CLAUDE_CODE_* vêm de uma sessão
      // do Claude Code que tenha iniciado a API e mudam o comportamento do processo filho.
      LocalCliProcess.Result result = LocalCliProcess.run(LABEL, command(chosenModel, level, systemPromptFile), workDir,
          userPrompt, LocalCliEfforts.timeoutOr(timeout, properties.timeout()),
          name -> name.startsWith("ANTHROPIC_") || name.equals("CLAUDECODE") || name.startsWith("CLAUDE_CODE_"));
      return read(result, level);
    } catch (IOException exception) {
      throw new IllegalStateException(LABEL + " could not be started (" + properties.command() + ")", exception);
    } finally {
      LocalCliProcess.deleteQuietly(workDir);
      oneAtATime.release();
    }
  }

  private List<String> command(String model, String effort, Path systemPromptFile) {
    return List.of(properties.command(), "-p",
        "--output-format", "json",
        "--model", model,
        "--effort", effort,
        "--tools", "",
        "--system-prompt-file", systemPromptFile.toString(),
        "--no-session-persistence",
        "--strict-mcp-config",
        "--setting-sources", "");
  }

  private String read(LocalCliProcess.Result result, String effort) {
    JsonNode json = parse(result.stdout());
    if (result.exitCode() != 0 || json == null || json.path("is_error").asBoolean(false)) {
      String detail = json != null && json.path("result").isTextual() ? json.path("result").asText() : result.stderr();
      log.warn("{} failed exit={} elapsedMs={} detail={}", LABEL, result.exitCode(), result.elapsedMillis(),
          LocalCliProcess.truncate(detail, MAX_ERROR_CHARS));
      throw new IllegalStateException(LABEL + " request failed (exit " + result.exitCode() + ")");
    }
    JsonNode text = json.path("result");
    if (!text.isTextual())
      throw new IllegalStateException(LABEL + " response did not contain a result");
    log.info("{} completed effort={} elapsedMs={}", LABEL, effort, result.elapsedMillis());
    return text.asText();
  }

  private JsonNode parse(String output) {
    if (output == null || output.isBlank())
      return null;
    try {
      JsonNode node = objectMapper.readTree(output);
      return node.isObject() ? node : null;
    } catch (JacksonException notJson) {
      return null;
    }
  }
}
