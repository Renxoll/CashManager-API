package pe.smartcash.cash.shared.interfaces.rest;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de keep-alive para planes free con cold start (Render, etc.): no toca DB, Redis
 * ni ningún puerto de dominio — solo construye un mapa literal, así que un ping cada 14
 * minutos desde UptimeRobot/cron-job.org no consume nada más que un hilo de Tomcat por un
 * instante. No usar {@code /actuator/health} para esto: ese sí evalúa los HealthIndicator
 * (DB, Redis) en cada llamada.
 */
@RestController
class SystemHealthController {

  @GetMapping("/api/v1/system/ping")
  Map<String, String> ping() {
    return Map.of("status", "UP");
  }
}
