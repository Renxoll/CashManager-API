package pe.smartcash.cash.iam.infrastructure.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * API stateless protegida por Bearer token: sign-up/sign-in son públicos (hay que poder
 * autenticarse sin estar ya autenticado), health también; todo lo demás exige un token
 * válido emitido por IAM.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
      BearerAuthenticationEntryPoint bearerAuthenticationEntryPoint,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/api/v1/iam/sign-up",
                        "/api/v1/iam/sign-in",
                        "/api/v1/iam/refresh",
                        "/api/v1/subscriptions/stripe-webhook",
                        // SendGrid Inbound Parse no manda Bearer (ni puede: no es un cliente
                        // de nuestra API); el token de query (?token=) en
                        // SendGridInboundWebhookController es la autenticación real acá,
                        // igual que la firma HMAC lo es para el webhook de Stripe arriba.
                        "/api/v1/transactions/inbound",
                        // Google redirige el navegador acá directo tras el consentimiento:
                        // no puede mandar un header Authorization en una navegación normal,
                        // así que el userId viaja indirecto vía "state" (ver
                        // GmailOAuthController / OAuthStateStore), no por Bearer token.
                        "/api/v1/gmail/oauth/callback",
                        "/api/v1/system/ping",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(bearerAuthenticationEntryPoint))
        .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Sin origen "*" a propósito: con credentials implícitas en el header Authorization, el
   * spec de CORS ni siquiera permite combinar "*" con un preflight que exponga cabeceras —
   * el navegador lo rechaza. Se lista cada origen explícito (dev de React/Vite + el dominio
   * real de producción) en vez de depender de un wildcard.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174",
            "https://app.smartcash.pe",
            "https://luki-web-application.vercel.app"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // Sin esto el navegador descarta la cabecera Location de las respuestas 201: por
    // default solo expone el puñado de cabeceras "seguras" del spec de CORS, y Location no
    // es una de ellas.
    configuration.setExposedHeaders(List.of("Location"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
