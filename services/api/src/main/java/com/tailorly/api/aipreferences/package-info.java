/**
 * Preferência de IA por conta para a extração do Currículo Base.
 *
 * <p>Uma conta sem {@link com.tailorly.api.aipreferences.AiPreferences} usa a configuração
 * global do servidor ({@code tailorly.interpretation.*}); {@link
 * com.tailorly.api.aipreferences.AiPreferencesService#resolveEffectiveSettings} resolve a
 * configuração efetiva campo a campo, consumida por
 * {@code com.tailorly.api.extraction.interpreting}. Chaves de API são criptografadas em
 * repouso via {@link com.tailorly.api.shared.security.SecretEncryptor} e nunca devolvidas em
 * texto puro pela API.</p>
 */
package com.tailorly.api.aipreferences;
