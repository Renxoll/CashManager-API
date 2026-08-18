package pe.smartcash.cash.transactions.infrastructure.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.services.NotificationTarget;
import pe.smartcash.cash.transactions.domain.services.TransactionNotifier;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "true")
class FcmTransactionNotifierAdapter implements TransactionNotifier {

  private final FirebaseMessaging firebaseMessaging;

  FcmTransactionNotifierAdapter(FirebaseMessaging firebaseMessaging) {
    this.firebaseMessaging = firebaseMessaging;
  }

  @Override
  public void notify(NotificationTarget target, String message) {
    Message fcmMessage =
        Message.builder()
            .setToken(target.fcmToken())
            .setNotification(Notification.builder().setTitle("Nuevo gasto registrado").setBody(message).build())
            .build();
    try {
      firebaseMessaging.send(fcmMessage);
    } catch (FirebaseMessagingException e) {
      // La transacción ya quedó persistida: una notificación fallida no debe tumbar
      // el caso de uso, solo se pierde el aviso push de ese evento.
      log.warn("No se pudo enviar la notificación push: {}", e.getMessage());
    }
  }
}
