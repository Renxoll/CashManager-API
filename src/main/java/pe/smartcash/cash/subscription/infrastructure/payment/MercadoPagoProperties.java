package pe.smartcash.cash.subscription.infrastructure.payment;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales y configuración de Mercado Pago. Sin validación estricta al arrancar (mismo
 * criterio que {@code CulqiProperties}): si falta el Access Token, el adaptador falla recién
 * cuando alguien intenta pagar (502).
 *
 * <p>La Public Key no vive acá: la usa solo el checkout del frontend (MercadoPago.js).
 *
 * @param apiBaseUrl     base del API ({@code https://api.mercadopago.com}); configurable para los tests.
 * @param accessToken    Access Token de la aplicación ({@code APP_USR-...} o {@code TEST-...}).
 * @param premiumPlanId  id del plan de suscripción (preapproval_plan) creado en Mercado Pago; opcional.
 * @param premiumAmount  monto mensual en {@code currency}; si es mayor que cero se envía en {@code auto_recurring}.
 * @param currency       moneda de cobro (PEN).
 * @param backUrl        URL https a la que Mercado Pago devuelve al usuario (obligatoria en su API).
 * @param webhookSecret  clave secreta de la firma de webhooks (panel de la aplicación > Webhooks).
 * @param timeout        timeout de conexión y lectura por llamada.
 */
@ConfigurationProperties(prefix = "app.mercadopago")
public record MercadoPagoProperties(
    String apiBaseUrl,
    String accessToken,
    String premiumPlanId,
    BigDecimal premiumAmount,
    String currency,
    String backUrl,
    String webhookSecret,
    Duration timeout) {}
