package pe.smartcash.cash.transactions.domain.services;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Puerto (Anti-Corruption Layer) hacia el bounded context Users. Deliberadamente separa
 * "¿existe el usuario?" de "¿tiene un destino de notificación?": un usuario puede existir
 * sin token FCM todavía, y eso no debe rechazar el registro del gasto.
 */
public interface UserDirectory {

  boolean exists(UserId userId);

  Optional<NotificationTarget> findNotificationTarget(UserId userId);

  /** Resuelve el dueño de una dirección de ingesta por correo (ver ingesta por SendGrid). */
  Optional<UserId> findUserIdByInboxAddress(String inboxAddress);
}
