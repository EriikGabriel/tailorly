package com.tailorly.api.shared.config;

import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import com.tailorly.api.session.AnonymousSessionAuthenticationFilter;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.session.AnonymousSessionTokens;


import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Configuração central de autenticação e autorização: sessões stateless, login com
 * {@code AuthenticationProvider} baseado em BCrypt e validação de Bearer JWT via
 * {@code oauth2ResourceServer}.
 *
 * <p>{@link #jwtAuthenticationConverter()} é o ponto mais sensível: a cada requisição
 * autenticada, ele consulta o usuário pelo id no sujeito do token e confirma que a conta ainda
 * está {@code enabled}, para que uma desativação tenha efeito imediato mesmo com um token
 * ainda dentro da validade — o JWT em si não é revogado, apenas deixa de autorizar.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
  private final UserRepository userRepository;
  private final LocalizedSecurityErrorHandler securityErrorHandler;

  /** Regras de autorização por rota; veja a classe para as regras de JWT e senha. */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<AnonymousSessionAuthenticationFilter> anonymousSessionFilter
  ) throws Exception {
    anonymousSessionFilter.ifAvailable(filter ->
        http.addFilterBefore(filter, BearerTokenAuthenticationFilter.class));
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(securityErrorHandler)
            .accessDeniedHandler(securityErrorHandler))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/users").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/auth/register", "/v1/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/owners").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/anonymous-sessions").permitAll()
            .requestMatchers("/v1/anonymous-sessions/current").authenticated()
            .requestMatchers(HttpMethod.GET, "/v1/anonymous-sessions/*").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/v1/anonymous-sessions/*/revoke").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/users", "/v1/users/by-email").hasRole("ADMIN")
            .requestMatchers(ANONYMOUS_ALLOWED).authenticated()
            .anyRequest().access(accountsOnly()))
        .httpBasic(AbstractHttpConfigurer::disable)
        .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenResolver(anonymousAwareBearerTokenResolver())
            .authenticationEntryPoint(securityErrorHandler)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  /**
   * Rotas de dados do Currículo Base que uma sessão anônima pode usar, restritas ao próprio
   * owner por {@code OwnerAccessService}, mais a leitura de vagas por link (sem dados de owner; o
   * limite de requisições da sessão e a validação anti-SSRF de {@code JobUrlPolicy} se aplicam).
   * Todo o resto exige uma conta: usuários, owners, objetos de storage, preferências de IA,
   * {@code /v1/me} e a migração para conta.
   */
  private static final String[] ANONYMOUS_ALLOWED = {
      "/v1/base-documents/upload", "/v1/base-documents/{id}", "/v1/base-documents/owner/{ownerId}",
      "/v1/base-versions/{id}",
      "/v1/extraction-runs", "/v1/extraction-runs/{id}", "/v1/extraction-runs/version/{versionId}",
      "/v1/extracted-pages/{id}", "/v1/extracted-pages/{id}/text", "/v1/extracted-pages/run/{runId}",
      "/v1/fact-records", "/v1/fact-records/{id}", "/v1/fact-records/version/{versionId}",
      "/v1/fact-records/{id}/display-order",
      "/v1/facts", "/v1/facts/{id}", "/v1/facts/record/{recordId}", "/v1/facts/{id}/review-state",
      "/v1/facts/{id}/confirm",
      "/v1/fact-evidence", "/v1/fact-evidence/{id}", "/v1/fact-evidence/fact/{factId}",
      "/v1/fact-evidence/{id}/locator",
      "/v1/review-issues/version/{versionId}", "/v1/review-issues/{id}/answer",
      "/v1/gt-snapshots/{id}", "/v1/gt-snapshots/approve-version/{versionId}",
      "/v1/job-postings", "/v1/job-postings/extract", "/v1/job-postings/owner/{ownerId}",
      "/v1/job-postings/{id}", "/v1/job-postings/{id}/versions/{versionNumber}",
      "/v1/job-postings/{id}/versions/{versionNumber}/analysis",
      "/v1/cv/generate", "/v1/cv/generations/owner/{ownerId}", "/v1/cv/generations/{id}",
      "/v1/cv/generations/{id}/download", "/v1/cv/generations/{id}/preview",
      "/v1/cv/templates", "/v1/cv/templates/bundled", "/v1/cv/templates/owner/{ownerId}/current",
      "/v1/cv/templates/{id}"
  };

  /** Autoriza qualquer autenticação, exceto a de uma sessão anônima. */
  private static AuthorizationManager<RequestAuthorizationContext> accountsOnly() {
    return (authentication, context) -> {
      Authentication current = authentication.get();
      boolean account = current != null
          && current.isAuthenticated()
          && !(current instanceof AnonymousAuthenticationToken)
          && !(current instanceof AnonymousSessionAuthenticationToken);
      return new AuthorizationDecision(account);
    };
  }

  /** Tokens {@code tlr_anon_} são tratados pelo filtro de sessão anônima, não como JWT. */
  private static BearerTokenResolver anonymousAwareBearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    return request -> {
      String token = delegate.resolve(request);
      return AnonymousSessionTokens.isAnonymousToken(token) ? null : token;
    };
  }

  /**
   * Converte um JWT validado em uma {@link Authentication} com as roles atuais do usuário,
   * rejeitando tokens de contas desativadas ou removidas.
   */
  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return token -> {
      UUID userId;
      try {
        userId = UUID.fromString(token.getSubject());
      } catch (IllegalArgumentException ex) {
        throw new BadCredentialsException("Invalid token subject", ex);
      }
      var user = userRepository.findById(userId)
          .filter(com.tailorly.api.user.User::isEnabled)
          .orElseThrow(() -> new BadCredentialsException("Account unavailable"));
      var authorities = user.getRoles().stream()
          .map(role -> new SimpleGrantedAuthority(role.getName().name()))
          .toList();
      return new JwtAuthenticationToken(token, authorities, user.getEmail());
    };
  }

  @Bean
  public SecretKey jwtSecretKey(@Value("${tailorly.security.jwt.secret}") String encodedSecret) {
    byte[] secret;
    try {
      secret = Base64.getDecoder().decode(encodedSecret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("TAILORLY_JWT_SECRET must be Base64 encoded", ex);
    }
    if (secret.length < 32) {
      throw new IllegalStateException("TAILORLY_JWT_SECRET must contain at least 32 bytes");
    }
    return new SecretKeySpec(secret, "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  public JwtDecoder jwtDecoder(
      SecretKey jwtSecretKey,
      @Value("${tailorly.security.jwt.issuer}") String issuer
  ) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return email -> userRepository.findByEmail(email)
        .map(user -> User.withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(user.getRoles().stream()
                .map(role -> role.getName().name())
                .toArray(String[]::new))
            .disabled(!user.isEnabled())
            .build())
        .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
            ErrorCodes.User.NOT_FOUND));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder
  ) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }
}
