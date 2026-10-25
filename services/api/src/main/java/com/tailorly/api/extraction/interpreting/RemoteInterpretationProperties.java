package com.tailorly.api.extraction.interpreting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do provedor remoto principal de interpretação.
 *
 * <p>O endpoint só precisa ser compatível com o formato Chat Completions da
 * OpenAI; o mesmo slot atende OpenAI, Anthropic, DeepSeek, OpenRouter ou
 * qualquer outro serviço que fale esse protocolo. Não há configuração
 * específica por fabricante — quem escolhe o fabricante é quem preenche
 * endpoint, modelo e chave.</p>
 */
@ConfigurationProperties(prefix = "tailorly.interpretation.remote")
public record RemoteInterpretationProperties(String endpoint, String model, String apiKey) {}
