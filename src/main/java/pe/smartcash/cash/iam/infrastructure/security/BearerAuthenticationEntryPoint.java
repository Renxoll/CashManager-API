package pe.smartcash.cash.iam.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import tools.jackson.databind.ObjectMapper;

/**
 * Sin esto, Spring Security responde 403 (Http403ForbiddenEntryPoint) a un request sin
 * token, que es semánticamente incorrecto: 403 dice "sé quién eres y no puedes", 401 dice
 * "no sé quién eres". Devuelve además el mismo formato {@link ApiError} que el resto de la API.
 */
@Component
class BearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  BearerAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws java.io.IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ApiError body = new ApiError(Instant.now(), 401, "Unauthorized", "Se requiere un token válido (Authorization: Bearer <token>)");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
