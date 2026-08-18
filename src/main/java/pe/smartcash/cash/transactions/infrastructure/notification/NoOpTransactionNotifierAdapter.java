package pe.smartcash.cash.transactions.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.services.NotificationTarget;
import pe.smartcash.cash.transactions.domain.services.TransactionNotifier;

/** Implementación por defecto: registra en log lo que se hubiera enviado, sin llamar a FCM. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
class NoOpTransactionNotifierAdapter implements TransactionNotifier {

  @Override
  public void notify(NotificationTarget target, String message) {
    log.info("[FCM mock, app.fcm.enabled=false] Se habría notificado a token={} -> \"{}\"", target.fcmToken(), message);
  }
}
