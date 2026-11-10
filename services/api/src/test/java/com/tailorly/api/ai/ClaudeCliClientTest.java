package com.tailorly.api.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import com.tailorly.api.aipreferences.EffectiveAiSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/** Usa scripts de shell no lugar do {@code claude} real: nada é enviado e nenhuma assinatura é consumida. */
class ClaudeCliClientTest {
  @TempDir
  static Path dir;

  @BeforeAll
  static void requiresPosixShell() {
    assumeTrue(Files.isExecutable(Path.of("/bin/sh")), "precisa de /bin/sh");
  }

  private static Path script(String name, String body) throws IOException {
    Path file = dir.resolve(name);
    Files.writeString(file, "#!/bin/sh\n" + body + "\n");
    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    return file;
  }

  private static ClaudeCliClient client(Path command, Duration timeout) {
    return new ClaudeCliClient(new ClaudeCliProperties(true, command.toString(), "sonnet", timeout, null, null), new ObjectMapper());
  }

  @Test
  void returnsResultAndPassesPromptsAndFlags() throws IOException {
    // Ecoa o stdin, os argumentos, o system prompt (arquivo) e se a chave de API chegou ao processo.
    Path fake = script("ok.sh", """
        input=$(cat)
        sys=""
        prev=""
        for a in "$@"; do
          if [ "$prev" = "--system-prompt-file" ]; then sys=$(cat "$a"); fi
          prev="$a"
        done
        key=${ANTHROPIC_API_KEY:-none}
        printf '{"type":"result","is_error":false,"result":"stdin=%s|args=%s|key=%s|sys=%s"}' "$input" "$*" "$key" "$(echo $sys | head -c 20)"
        """);

    String result = client(fake, Duration.ofSeconds(10)).complete(null, "Sistema X", "texto do curriculo");

    assertTrue(result.contains("stdin=texto do curriculo"), result);
    assertTrue(result.contains("-p --output-format json --model sonnet --effort medium --tools"), result);
    assertTrue(result.contains("--no-session-persistence --strict-mcp-config --setting-sources"), result);
    assertTrue(result.contains("key=none"), result);
    assertTrue(result.contains("sys=Sistema X"), result);
  }

  @Test
  void effortPerTaskUsesConfiguredThenPresetThenDefault() {
    ClaudeCliProperties properties = new ClaudeCliProperties(true, null, null, null, "low",
        java.util.Map.of("job-analysis", "xhigh", "due-diligence", "invalido"));

    assertEquals("xhigh", properties.effortFor("job analysis"));
    assertEquals("high", properties.effortFor("interpretation"));
    assertEquals("medium", properties.effortFor("due diligence"));
    assertEquals("low", properties.effortFor("resume layout plan"));
    assertEquals("low", properties.effortFor("tarefa nova"));
  }

  @Test
  void chatClientSendsTheTaskEffortToTheCli() throws IOException {
    Path fake = script("effort.sh", """
        cat >/dev/null
        prev=""
        for a in "$@"; do
          if [ "$prev" = "--effort" ]; then e="$a"; fi
          prev="$a"
        done
        printf '{"is_error":false,"result":"%s"}' "$e"
        """);
    AiChatClient chat = new AiChatClient(client(fake, Duration.ofSeconds(10)));
    EffectiveAiSettings settings = new EffectiveAiSettings("claude-cli", null, null, null, null, null, null);

    assertEquals("high", chat.run(settings, "interpretation", config -> chat.complete(config, "s", "u")));
    assertEquals("low", chat.run(settings, "resume layout plan", config -> chat.complete(config, "s", "u")));
    assertEquals("medium", chat.run(settings, "job analysis", config -> chat.complete(config, "s", "u")));
  }

  @Test
  void accountOverridesWinOverServerEffortsModelAndTimeout() throws IOException {
    Path fake = script("override.sh", """
        cat >/dev/null
        prev=""
        for a in "$@"; do
          if [ "$prev" = "--effort" ]; then e="$a"; fi
          if [ "$prev" = "--model" ]; then m="$a"; fi
          prev="$a"
        done
        printf '{"is_error":false,"result":"%s/%s"}' "$m" "$e"
        """);
    AiChatClient chat = new AiChatClient(client(fake, Duration.ofSeconds(10)));
    EffectiveAiSettings settings = new EffectiveAiSettings("claude-cli", null, null, null, null, null, null,
        new EffectiveAiSettings.LocalCliOverrides("opus", Duration.ofSeconds(60),
            java.util.Map.of("job-analysis", "max", "default", "low")), null);

    assertEquals("opus/max", chat.run(settings, "job analysis", config -> chat.complete(config, "s", "u")));
    // Etapa sem valor da conta: vale o padrão do servidor para ela, não o "default" da conta.
    assertEquals("opus/high", chat.run(settings, "interpretation", config -> chat.complete(config, "s", "u")));
    // Etapa desconhecida (sem padrão no servidor): vale o "default" da conta.
    assertEquals("opus/low", chat.run(settings, "nova etapa", config -> chat.complete(config, "s", "u")));
  }

  @Test
  void failsWhenTheCliReportsAnError() throws IOException {
    Path fake = script("error.sh", """
        cat >/dev/null
        printf '{"type":"result","is_error":true,"result":"Not logged in"}'
        exit 1
        """);

    assertThrows(IllegalStateException.class, () -> client(fake, Duration.ofSeconds(10)).complete("m", "s", "u"));
  }

  @Test
  void failsWhenTheOutputIsNotJson() throws IOException {
    Path fake = script("garbage.sh", "cat >/dev/null; echo oops");

    assertThrows(IllegalStateException.class, () -> client(fake, Duration.ofSeconds(10)).complete("m", "s", "u"));
  }

  @Test
  void killsTheProcessOnTimeout() throws IOException {
    Path fake = script("slow.sh", "cat >/dev/null; sleep 30");

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> client(fake, Duration.ofMillis(300)).complete("m", "s", "u"));
    assertTrue(error.getMessage().contains("timed out"));
  }

  @Test
  void disabledByDefault() {
    ClaudeCliClient disabled = new ClaudeCliClient(ClaudeCliProperties.disabled(), new ObjectMapper());

    assertFalse(disabled.enabled());
    assertThrows(IllegalStateException.class, () -> disabled.complete("m", "s", "u"));
  }

  @Test
  void chatClientRoutesClaudeCliProviderAndRefusesWhenDisabled() throws IOException {
    Path fake = script("route.sh", "cat >/dev/null; printf '{\"is_error\":false,\"result\":\"{}\"}'");
    EffectiveAiSettings settings = new EffectiveAiSettings("claude-cli", null, null, null, null, null, null);

    AiChatClient enabled = new AiChatClient(client(fake, Duration.ofSeconds(10)));
    assertEquals("{}", enabled.run(settings, "test", config -> enabled.complete(config, "s", "u")));

    AiChatClient withoutCli = new AiChatClient();
    assertThrows(IllegalStateException.class,
        () -> withoutCli.run(settings, "test", config -> withoutCli.complete(config, "s", "u")));
  }
}
