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
import java.util.Map;

import com.tailorly.api.aipreferences.EffectiveAiSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Usa scripts de shell no lugar do {@code codex} real: nada é enviado e nenhuma cota é consumida. */
class CodexCliClientTest {
  @TempDir
  static Path dir;

  // Grava a última mensagem em --output-last-message, como o Codex faz.
  private static final String WRITE_LAST = """
      out=""
      prev=""
      for a in "$@"; do
        if [ "$prev" = "--output-last-message" ]; then out="$a"; fi
        prev="$a"
      done
      """;

  @BeforeAll
  static void requiresPosixShell() {
    assumeTrue(Files.isExecutable(Path.of("/bin/sh")), "precisa de /bin/sh");
  }

  private static Path script(String name, String body) throws IOException {
    Path file = dir.resolve(name);
    Files.writeString(file, "#!/bin/sh\n" + WRITE_LAST + body + "\n");
    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    return file;
  }

  private static CodexCliClient client(Path command, String model, Duration timeout) {
    return new CodexCliClient(new CodexCliProperties(true, command.toString(), model, timeout, null, null));
  }

  @Test
  void returnsTheLastMessageAndRunsWithoutAgentTools() throws IOException {
    Path fake = script("ok.sh", """
        input=$(cat)
        key=${OPENAI_API_KEY:-none}
        case "$input" in *"<instrucoes>"*"Sistema X"*"<entrada>"*"texto do curriculo"*) shape=ok ;; *) shape=bad ;; esac
        printf 'args=%s|key=%s|shape=%s' "$*" "$key" "$shape" > "$out"
        """);

    String result = client(fake, "", Duration.ofSeconds(10)).complete(null, "high", null, "Sistema X", "texto do curriculo");

    assertTrue(result.startsWith("args=exec --skip-git-repo-check --ephemeral --ignore-user-config --ignore-rules"), result);
    assertTrue(result.contains("--sandbox read-only"), result);
    assertTrue(result.contains("approval_policy=\"never\""), result);
    assertTrue(result.contains("web_search=\"disabled\""), result);
    assertTrue(result.contains("model_reasoning_effort=\"high\""), result);
    assertTrue(result.contains("--disable shell_tool"), result);
    assertTrue(result.contains("--disable unified_exec"), result);
    assertFalse(result.contains("--model"), "modelo vazio usa o padrão do Codex: " + result);
    assertTrue(result.contains("key=none"), result);
    assertTrue(result.contains("shape=ok"), result);
  }

  @Test
  void passesTheModelAndMapsMaxToXhigh() throws IOException {
    Path fake = script("model.sh", "cat >/dev/null; printf '%s' \"$*\" > \"$out\"");

    String result = client(fake, "gpt-test", Duration.ofSeconds(10)).complete(null, "max", null, "s", "u");

    assertTrue(result.contains("--model gpt-test"), result);
    assertTrue(result.contains("model_reasoning_effort=\"xhigh\""), result);
  }

  @Test
  void reportsOnlyErrorLinesWhenTheCliFails() throws IOException {
    // O Codex repete o prompt no stderr; o currículo não pode ir para a mensagem de erro.
    Path fake = script("limit.sh", """
        cat >&2
        echo "ERROR: You've hit your usage limit." >&2
        exit 1
        """);

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> client(fake, "", Duration.ofSeconds(10)).complete(null, null, null, "s", "dados pessoais do curriculo"));
    assertTrue(error.getMessage().contains("usage limit"), error.getMessage());
    assertFalse(error.getMessage().contains("dados pessoais"), error.getMessage());
  }

  @Test
  void failsWhenThereIsNoLastMessage() throws IOException {
    Path fake = script("empty.sh", "cat >/dev/null; exit 0");

    assertThrows(IllegalStateException.class,
        () -> client(fake, "", Duration.ofSeconds(10)).complete(null, null, null, "s", "u"));
  }

  @Test
  void killsTheProcessOnTimeout() throws IOException {
    Path fake = script("slow.sh", "cat >/dev/null; sleep 30");

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> client(fake, "", Duration.ofMillis(300)).complete(null, null, Duration.ofMillis(300), "s", "u"));
    assertTrue(error.getMessage().contains("timed out"), error.getMessage());
  }

  @Test
  void chatClientRoutesCodexWithAccountOverrides() throws IOException {
    Path fake = script("route.sh", """
        cat >/dev/null
        m="default"
        p2=""
        for a in "$@"; do
          if [ "$p2" = "--model" ]; then m="$a"; fi
          case "$a" in model_reasoning_effort=*) e="$a" ;; esac
          p2="$a"
        done
        printf '%s %s' "$m" "$e" > "$out"
        """);
    AiChatClient chat = new AiChatClient(null, client(fake, "", Duration.ofSeconds(10)));
    EffectiveAiSettings settings = new EffectiveAiSettings("codex-cli", null, null, null, null, null, null, null,
        new EffectiveAiSettings.LocalCliOverrides("gpt-conta", null, Map.of("job-analysis", "low")));

    assertEquals("gpt-conta model_reasoning_effort=\"low\"",
        chat.run(settings, "job analysis", config -> chat.complete(config, "s", "u")));
    assertEquals("gpt-conta model_reasoning_effort=\"high\"",
        chat.run(settings, "interpretation", config -> chat.complete(config, "s", "u")));

    AiChatClient withoutCodex = new AiChatClient();
    assertThrows(IllegalStateException.class,
        () -> withoutCodex.run(settings, "job analysis", config -> withoutCodex.complete(config, "s", "u")));
  }

  @Test
  void disabledByDefault() {
    CodexCliClient disabled = new CodexCliClient(CodexCliProperties.disabled());

    assertFalse(disabled.enabled());
    assertThrows(IllegalStateException.class, () -> disabled.complete(null, null, null, "s", "u"));
  }
}
