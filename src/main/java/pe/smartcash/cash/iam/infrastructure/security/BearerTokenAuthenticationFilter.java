package pe.smartcash.cash.iam.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.smartcash.cash.iam.domain.services.TokenBlacklistService;
import pe.smartcash.cash.iam.domain.services.TokenService;

/**
 * Valida el header {@code Authorization: Bearer <token>} y, si es válido y no está en la
 * blacklist de logout, autentica el request con el userId como principal. Si falta, es
 * inválido, o fue revocado, simplemente deja el request sin autenticar: es la regla de
 * autorización en {@code SecurityConfig} la que decide qué rutas exigen autenticación.
 */
@Component
class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenService tokenService;
  private final TokenBlacklistService tokenBlacklistService;

  BearerTokenAuthenticationFilter(TokenService tokenService, TokenBlacklistService tokenBlacklistService) {
    this.tokenService = tokenService;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      // La firma se valida primero: no tiene sentido pagar el round-trip a Redis para un
      // token que ya es inválido o está expirado por sí solo.
      tokenService
          .validate(token)
          .filter(userId -> !tokenBlacklistService.isBlacklisted(token))
          .ifPresent(
              userId -> {
                var authentication = new UsernamePasswordAuthenticationToken(userId.value().toString(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }
    filterChain.doFilter(request, response);
  }
}
