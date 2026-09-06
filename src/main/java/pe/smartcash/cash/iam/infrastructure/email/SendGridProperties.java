package pe.smartcash.cash.iam.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sin validación estricta a propósito (mismo criterio que {@code StripeProperties}): el
 * envío de correo es una integración opcional. Con {@code enabled=false} (default) ni
 * siquiera se instancia el adapter de SendGrid -- entra {@code
 * LoggingPasswordResetEmailAdapter}, que solo escribe el enlace en el log.
 */
@ConfigurationProperties(prefix = "app.sendgrid")
public record SendGridProperties(boolean enabled, String apiKey, String fromEmail, String fromName) {}
