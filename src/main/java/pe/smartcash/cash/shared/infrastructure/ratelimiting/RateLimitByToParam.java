package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de controller cuyo rate limit se calcula por el valor de su parámetro
 * {@code @RequestParam("to")}, no por IP ni por usuario autenticado -- pensado para webhooks
 * como SendGrid Inbound Parse, donde el caller HTTP es siempre la infraestructura del
 * proveedor (misma IP para todos los tenants) y la única identidad real disponible es un
 * campo del propio payload. Ver {@link WebhookRateLimitingAspect}.
 *
 * <p>El nombre fija el parámetro a {@code "to"} a propósito, en vez de aceptar un nombre
 * configurable: hoy solo existe un caso de uso ({@code SendGridInboundWebhookController}) y
 * generalizar de más ahora sería resolver un problema que todavía no existe.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimitByToParam {}
