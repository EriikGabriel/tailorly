package com.tailorly.api.shared.config;

import java.util.Base64;
import java.util.UUID;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
  private final UserRepository userRepository;
  private final LocalizedSecurityErrorHandler securityErrorHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
            .requestMatchers(
                HttpMethod.POST,
                "/v1/anonymous-sessions",
                "/v1/anonymous-sessions/validate"
            ).permitAll()
            .requestMatchers(HttpMethod.GET, "/v1/anonymous-sessions/*").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/v1/anonymous-sessions/*/revoke").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/users", "/v1/users/by-email").hasRole("ADMIN")
            .anyRequest().authenticated())
        .httpBasic(AbstractHttpConfigurer::disable)
        .oauth2ResourceServer(oauth2 -> oauth2
            .authenticationEntryPoint(securityErrorHandler)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

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
