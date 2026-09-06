package pe.smartcash.cash.iam.infrastructure.email;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.services.PasswordResetEmailNotifier;

/**
 * Implementación por defecto (app.sendgrid.enabled=false o ausente): no envía nada, solo
 * deja el enlace en el log para poder probar el flujo completo en dev/CI sin API key.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.sendgrid", name = "enabled", havingValue = "false", matchIfMissing = true)
class LoggingPasswordResetEmailAdapter implements PasswordResetEmailNotifier {

  @Override
  public void sendResetLink(Email recipient, String resetUrl, Duration validFor) {
    log.info(
        "[SendGrid mock, app.sendgrid.enabled=false] Enlace de restablecimiento para {} (vale {} min): {}",
        recipient.value(),
        validFor.toMinutes(),
        resetUrl);
  }
}
