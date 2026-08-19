package pe.smartcash.cash.shared.infrastructure.observability;

import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Un id por request, transversal a todos los bounded contexts. Se toma de {@code
 * X-Correlation-Id} si el caller ya trae uno (útil cuando el frontend o un proxy lo generó
 * antes), o se genera acá si no. Vive en MDC para que todo log de ese hilo lo incluya (ver
 * {@code logging.pattern.console} en application.properties) y en el scope de Sentry para
 * que cualquier evento capturado durante el request quede taggeado con el mismo id — así un
 * error en Sentry se puede cruzar con las líneas de log del mismo request.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String MDC_KEY = "correlationId";
  private static final String HEADER = "X-Correlation-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = request.getHeader(HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    response.setHeader(HEADER, correlationId);
    MDC.put(MDC_KEY, correlationId);
    try {
      Sentry.configureScope(scope -> scope.setTag("correlation_id", MDC.get(MDC_KEY)));
      filterChain.doFilter(request, response);
    } finally {
      // Los hilos de Tomcat se reciclan entre requests: sin este cleanup, el MDC de un
      // request quedaría "pegado" en logs de requests posteriores atendidos por el mismo hilo.
      MDC.remove(MDC_KEY);
    }
  }
}
