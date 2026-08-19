package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AOP en vez de un {@code Filter} de servlet: un filtro corre ANTES de que Spring MVC
 * resuelva los {@code @RequestParam} del {@code multipart/form-data} del webhook, así que
 * leer "to" ahí implicaría parsear el body multipart una segunda vez (una en el filtro, otra
 * en el controller) o consumir el stream antes de que el controller lo vea. Interceptando el
 * método del controller en cambio, el parámetro ya viene resuelto por Spring -- cero parsing
 * extra, cero riesgo de dejar el stream ya leído.
 */
@Aspect
@Component
class WebhookRateLimitingAspect {

  private final WebhookRateLimiterService rateLimiterService;

  WebhookRateLimitingAspect(WebhookRateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
  }

  @Around("@annotation(pe.smartcash.cash.shared.infrastructure.ratelimiting.RateLimitByToParam)")
  Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
    String to = extractToParam(joinPoint);
    rateLimiterService.consume(to);
    return joinPoint.proceed();
  }

  private String extractToParam(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Parameter[] parameters = method.getParameters();
    Object[] args = joinPoint.getArgs();

    for (int i = 0; i < parameters.length; i++) {
      if (isBoundToHttpParam(parameters[i], "to")) {
        return (String) args[i];
      }
    }
    throw new IllegalStateException(
        "@RateLimitByToParam requiere un parámetro @RequestParam(\"to\") en " + method.getDeclaringClass().getSimpleName() + "."
            + method.getName());
  }

  /**
   * Coincide por el nombre HTTP declarado en {@code @RequestParam("nombre")}, no por el
   * nombre del parámetro Java: es lo mismo que usa Spring MVC para bindear, así que no
   * depende de que el proyecto compile con {@code -parameters} (lo hace, vía el plugin de
   * Spring Boot, pero no hay que confiar en eso implícitamente acá).
   */
  private boolean isBoundToHttpParam(Parameter parameter, String httpParamName) {
    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
    if (requestParam == null) {
      return false;
    }
    String boundName = requestParam.value().isBlank() ? parameter.getName() : requestParam.value();
    return httpParamName.equals(boundName);
  }
}
