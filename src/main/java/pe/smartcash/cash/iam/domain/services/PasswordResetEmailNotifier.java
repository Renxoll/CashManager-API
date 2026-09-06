package pe.smartcash.cash.iam.domain.services;

import java.time.Duration;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;

/**
 * Puerto de salida hacia el proveedor de correo transaccional. La implementación por
 * defecto no envía nada (solo loguea el enlace, para dev/CI sin API key); la real habla
 * con SendGrid. Un fallo de envío no debe alterar la respuesta del endpoint que lo dispara
 * (anti-enumeración), así que las implementaciones no relanzan: registran y alertan.
 */
public interface PasswordResetEmailNotifier {

  void sendResetLink(Email recipient, String resetUrl, Duration validFor);
}
