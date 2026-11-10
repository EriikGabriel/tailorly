package com.tailorly.api.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Executa {@code codex exec} (Codex CLI em modo não interativo) com a conta ChatGPT já logada no CLI.
 * O consumo entra nos limites dessa conta, por isso o provider é só para uso local e fica desligado
 * por padrão ({@code TAILORLY_CODEX_CLI_ENABLED}).
 *
 * <p>O Codex é um agente com ferramentas; aqui ele roda com sandbox somente leitura, sem aprovações,
 * sem configuração/regras do usuário, sem sessão salva e com shell, navegador, plugins, apps, MCP de
 * apps, geração de imagem, subagentes e busca na web desligados, para só responder ao prompt. O Codex não
 * tem prompt de sistema separado: as instruções vão antes da entrada, no mesmo stdin.
 */
@Component
@EnableConfigurationProperties(CodexCliProperties.class)
public class CodexCliClient {
  private static final Logger log = LoggerFactory.getLogger(CodexCliClient.class);
  private static final String LABEL = "Codex CLI";
  private static final int MAX_ERROR_CHARS = 500;
  private static final String JSON_ONLY =
      "\n\nResponda somente com o JSON pedido: sem texto antes ou depois e sem cercas de markdown. "
          + "Não use ferramentas, não leia arquivos e não execute comandos; responda só com base na entrada.";
  /** Recursos de agente desligados; nomes de {@code codex features list}. */
  private static final List<String> DISABLED_FEATURES = List.of(
      "shell_tool", "unified_exec", "apps", "plugins", "hooks", "browser_use", "browser_use_external",
      "computer_use", "image_generation", "multi_agent", "view_image");

  private final CodexCliProperties properties;
  private final Semaphore oneAtATime = new Semaphore(1, true);

  public CodexCliClient(CodexCliProperties properties) {
    this.properties = properties;
  }

  public boolean enabled() {
    return properties.enabled();
  }

  public String model() {
    return properties.model();
  }

  public CodexCliProperties properties() {
    return properties;
  }

  /** Esforço da etapa, conforme {@link LocalCliProperties#effortFor(String, Map)}. */
  public String effortFor(String task, Map<String, String> accountEfforts) {
    return properties.effortFor(task, accountEfforts);
  }

  /** {@code effort} nulo ou inválido e {@code timeout} nulo ou fora dos limites usam o servidor. */
  public String complete(String model, String effort, Duration timeout, String systemPrompt, String userPrompt) {
    if (!properties.enabled())
      throw new IllegalStateException(LABEL + " provider is disabled (set TAILORLY_CODEX_CLI_ENABLED=true for local use)");

    try {
      oneAtATime.acquire();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the " + LABEL, interrupted);
    }

    Path workDir = null;
    try {
      workDir = LocalCliProcess.createWorkDir("tailorly-codex-cli-");
      Path lastMessage = workDir.resolve("last-message.txt");
      String level = LocalCliEfforts.normalize(effort) != null ? LocalCliEfforts.normalize(effort) : properties.effort();
      String chosenModel = model == null || model.isBlank() ? properties.model() : model.trim();
      String prompt = "<instrucoes>\n" + (systemPrompt == null ? "" : systemPrompt) + JSON_ONLY
          + "\n</instrucoes>\n\n<entrada>\n" + (userPrompt == null ? "" : userPrompt) + "\n</entrada>\n";

      // OPENAI_*/CODEX_* (chave, base URL, perfis) fariam o CLI cobrar a API ou ir para outro endpoint em
      // vez de usar a conta logada; CODEX_HOME fica, porque é onde está o login.
      LocalCliProcess.Result result = LocalCliProcess.run(LABEL, command(chosenModel, level, workDir, lastMessage),
          workDir, prompt, LocalCliEfforts.timeoutOr(timeout, properties.timeout()),
          name -> name.startsWith("OPENAI_") || (name.startsWith("CODEX_") && !name.equals("CODEX_HOME")));
      return read(result, lastMessage, level);
    } catch (IOException exception) {
      throw new IllegalStateException(LABEL + " could not be started (" + properties.command() + ")", exception);
    } finally {
      LocalCliProcess.deleteQuietly(workDir);
      oneAtATime.release();
    }
  }

  /** O Codex aceita até {@code xhigh}; {@code max} do Tailorly vira {@code xhigh}. */
  static String codexEffort(String level) {
    return "max".equals(level) ? "xhigh" : level;
  }

  private List<String> command(String model, String effort, Path workDir, Path lastMessage) {
    List<String> command = new ArrayList<>(List.of(properties.command(), "exec",
        "--skip-git-repo-check", "--ephemeral", "--ignore-user-config", "--ignore-rules",
        "--sandbox", "read-only", "--color", "never",
        "--cd", workDir.toString(),
        "-c", "approval_policy=\"never\"",
        "-c", "web_search=\"disabled\"",
        "-c", "model_reasoning_effort=\"" + codexEffort(effort) + "\"",
        "--output-last-message", lastMessage.toString()));
    DISABLED_FEATURES.forEach(feature -> {
      command.add("--disable");
      command.add(feature);
    });
    if (!model.isBlank()) {
      command.add("--model");
      command.add(model);
    }
    // "-" = instruções pelo stdin.
    command.add("-");
    return command;
  }

  private String read(LocalCliProcess.Result result, Path lastMessage, String effort) throws IOException {
    String message = Files.exists(lastMessage) ? Files.readString(lastMessage, StandardCharsets.UTF_8).strip() : "";
    if (result.exitCode() != 0 || message.isEmpty()) {
      // O Codex repete o prompt (com o currículo) no stderr; só as linhas de erro vão para o log.
      String errors = result.stderr().lines().filter(line -> line.startsWith("ERROR"))
          .distinct().collect(Collectors.joining(" | "));
      log.warn("{} failed exit={} elapsedMs={} detail={}", LABEL, result.exitCode(), result.elapsedMillis(),
          LocalCliProcess.truncate(errors, MAX_ERROR_CHARS));
      throw new IllegalStateException(LABEL + " request failed (exit " + result.exitCode() + ")"
          + (errors.isEmpty() ? "" : ": " + LocalCliProcess.truncate(errors, MAX_ERROR_CHARS)));
    }
    log.info("{} completed effort={} elapsedMs={}", LABEL, effort, result.elapsedMillis());
    return message;
  }
}
