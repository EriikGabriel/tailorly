package com.tailorly.api.cv.defaults.dto;

import java.util.List;

/**
 * Padrões da conta, usados como ponto de partida de cada geração (o gerador pode ajustar diretrizes
 * e layout por vaga sem alterá-los). {@code templateName} é nulo quando a conta ainda não escolheu
 * um layout; a otimização para ATS vem ligada e as diretrizes vêm com o conjunto padrão até a conta mudar.
 */
public record GenerationDefaultsResponse(String templateName, boolean optimizeForAts, List<String> guidelines) {
}
