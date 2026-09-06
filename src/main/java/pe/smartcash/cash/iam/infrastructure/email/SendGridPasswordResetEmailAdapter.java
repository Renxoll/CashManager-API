package pe.smartcash.cash.iam.infrastructure.email;

import io.sentry.Sentry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.services.PasswordResetEmailNotifier;

/**
 * Envía el correo de restablecimiento vía la API v3 de SendGrid ({@code POST
 * /v3/mail/send}). Único punto que conoce el shape del payload de SendGrid.
 *
 * <p>No relanza ante un fallo de envío: el endpoint que lo dispara responde igual exista o
 * no la cuenta (anti-enumeración), así que un 4xx/5xx de SendGrid no puede cambiar esa
 * respuesta -- se registra y se manda a Sentry, y el usuario simplemente no recibe el
 * correo (podrá volver a pedirlo).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.sendgrid", name = "enabled", havingValue = "true")
class SendGridPasswordResetEmailAdapter implements PasswordResetEmailNotifier {

  private static final String SUBJECT = "Restablece tu contraseña de Luki";

  private final RestClient restClient;
  private final SendGridProperties properties;

  SendGridPasswordResetEmailAdapter(RestClient.Builder restClientBuilder, SendGridProperties properties) {
    this.restClient =
        restClientBuilder
            .baseUrl("https://api.sendgrid.com")
            .defaultHeader("Authorization", "Bearer " + properties.apiKey())
            .build();
    this.properties = properties;
  }

  @Override
  public void sendResetLink(Email recipient, String resetUrl, Duration validFor) {
    Map<String, Object> body =
        Map.of(
            "personalizations", List.of(Map.of("to", List.of(Map.of("email", recipient.value())))),
            "from", Map.of("email", properties.fromEmail(), "name", properties.fromName()),
            "subject", SUBJECT,
            "content",
                List.of(
                    Map.of("type", "text/plain", "value", plainTextBody(resetUrl, validFor)),
                    Map.of("type", "text/html", "value", htmlBody(resetUrl, validFor))));

    try {
      restClient.post().uri("/v3/mail/send").body(body).retrieve().toBodilessEntity();
    } catch (RuntimeException e) {
      log.error("No se pudo enviar el correo de restablecimiento a {}: {}", recipient.value(), e.getMessage());
      Sentry.captureException(e, scope -> scope.setTag("component", "sendgrid-password-reset"));
    }
  }

  private static String plainTextBody(String resetUrl, Duration validFor) {
    return """
        Pediste restablecer tu contraseña de Luki.

        Abre este enlace para elegir una nueva (vale %d minutos):
        %s

        Si no fuiste tú, ignora este correo: tu contraseña no cambia hasta que uses el enlace.
        """
        .formatted(validFor.toMinutes(), resetUrl);
  }

  private static String htmlBody(String resetUrl, Duration validFor) {
    return """
        <p>Pediste restablecer tu contraseña de <strong>Luki</strong>.</p>
        <p><a href="%s">Elegir una contraseña nueva</a> (el enlace vale %d minutos).</p>
        <p>Si no fuiste tú, ignora este correo: tu contraseña no cambia hasta que uses el enlace.</p>
        """
        .formatted(resetUrl, validFor.toMinutes());
  }
}
