package pe.smartcash.cash.subscription.infrastructure.payment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales y configuración de Culqi. Sin validación estricta al arrancar a propósito: los
 * pagos son opcionales para levantar la app en dev/CI; si falta la llave, el adaptador falla
 * recién cuando alguien intenta pagar (502).
 *
 * <p>La llave pública ({@code pk_...}) no vive acá: la usa solo el checkout del frontend.
 *
 * @param apiBaseUrl     base del API REST de Culqi ({@code https://api.culqi.com/v2}); configurable para los tests.
 * @param secretKey      llave secreta {@code sk_test_...} o {@code sk_live_...} del CulqiPanel.
 * @param premiumPlanId  id del plan PREMIUM creado una sola vez en el CulqiPanel ({@code pln_...}).
 * @param timeout        timeout de conexión y lectura por llamada.
 */
@ConfigurationProperties(prefix = "app.culqi")
public record CulqiProperties(String apiBaseUrl, String secretKey, String premiumPlanId, Duration timeout) {}
