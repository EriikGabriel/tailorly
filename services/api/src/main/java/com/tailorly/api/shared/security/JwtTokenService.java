package com.tailorly.api.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.tailorly.api.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite JWTs Bearer assinados com HS256 após um login válido.
 *
 * <p>O sujeito do token é o id do usuário; emissão e expiração usam {@code tailorly.security.jwt.issuer}
 * e {@code tailorly.security.jwt.ttl} (1 hora por padrão). Não há refresh token, logout nem
 * revogação individual: trocar a chave de assinatura ({@code TAILORLY_JWT_SECRET}) invalida
 * todos os tokens já emitidos. Cada chamada autenticada revalida conta e roles atuais (veja
 * {@link com.tailorly.api.shared.config.SecurityConfiguration}), então uma conta desativada
 * perde acesso imediatamente mesmo com um token ainda válido.</p>
 */
@Service
public class JwtTokenService {
  private final JwtEncoder jwtEncoder;
  private final String issuer;
  private final Duration ttl;

  public JwtTokenService(
      JwtEncoder jwtEncoder,
      @Value("${tailorly.security.jwt.issuer}") String issuer,
      @Value("${tailorly.security.jwt.ttl}") Duration ttl
  ) {
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.ttl = ttl;
  }

  /** Gera o token de acesso e a resposta de login completa para o usuário autenticado. */
  public LoginResponse issue(UserResponse user) {
    Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant expiresAt = issuedAt.plus(ttl);
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject(user.id().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .build();
    String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    return LoginResponse.builder()
        .accessToken(accessToken)
        .tokenType("Bearer")
        .expiresAt(expiresAt)
        .user(user)
        .build();
  }
}
