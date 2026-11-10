package com.tailorly.api.aipreferences;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Preferência de IA de um owner para a extração do Currículo Base.
 *
 * <p>Os campos {@code remote*}/{@code ollama*} espelham {@code RemoteInterpretationProperties}
 * e {@code OllamaInterpretationProperties}: o mesmo slot "remoto" atende qualquer serviço
 * compatível com o formato Chat Completions (OpenAI, Anthropic, DeepSeek, OpenRouter etc.), não
 * há configuração específica por fabricante. Chaves de API nunca são guardadas em texto puro —
 * veja {@link com.tailorly.api.shared.security.SecretEncryptor}. Um owner sem registro aqui usa
 * a configuração global do servidor ({@code tailorly.interpretation.*}); ver
 * {@link AiPreferencesService#resolveEffectiveSettings}.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "ai_preferences")
public class AiPreferences {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @OneToOne(optional = false)
  @JoinColumn(name = "owner_id", nullable = false, unique = true)
  private Owner owner;

  @Column(name = "provider", nullable = false)
  private String provider;

  @Column(name = "remote_endpoint")
  private String remoteEndpoint;

  @Column(name = "remote_model")
  private String remoteModel;

  @ToString.Exclude
  @Column(name = "remote_api_key_encrypted")
  private String remoteApiKeyEncrypted;

  @Column(name = "ollama_endpoint")
  private String ollamaEndpoint;

  @Column(name = "ollama_model")
  private String ollamaModel;

  @ToString.Exclude
  @Column(name = "ollama_api_key_encrypted")
  private String ollamaApiKeyEncrypted;

  /** Alias do CLI ({@code sonnet}, {@code opus}, {@code haiku}) ou nome completo; nulo usa o do servidor. */
  @Column(name = "claude_cli_model")
  private String claudeCliModel;

  /** Nulo usa o timeout do servidor ({@code TAILORLY_CLAUDE_CLI_TIMEOUT}). */
  @Column(name = "claude_cli_timeout_seconds")
  private Integer claudeCliTimeoutSeconds;

  /** Esforço por etapa ({@code default} e chaves de {@code ClaudeCliProperties#TASKS}). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "claude_cli_efforts", columnDefinition = "jsonb")
  private Map<String, String> claudeCliEfforts;

  /** Modelo do Codex; nulo usa o do servidor (que, vazio, usa o padrão do próprio Codex). */
  @Column(name = "codex_cli_model")
  private String codexCliModel;

  /** Nulo usa o timeout do servidor ({@code TAILORLY_CODEX_CLI_TIMEOUT}). */
  @Column(name = "codex_cli_timeout_seconds")
  private Integer codexCliTimeoutSeconds;

  /** Esforço por etapa ({@code default} e chaves de {@code LocalCliEfforts#TASKS}). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "codex_cli_efforts", columnDefinition = "jsonb")
  private Map<String, String> codexCliEfforts;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
