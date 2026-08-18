package pe.smartcash.cash.subscription.domain.exception;

import java.util.UUID;

/** Se usa tanto para "no existe esa suscripción" como para "el usuario no tiene una suscripción activa". */
public class SubscriptionNotFoundException extends RuntimeException {

  private SubscriptionNotFoundException(String message) {
    super(message);
  }

  public static SubscriptionNotFoundException byId(UUID subscriptionId) {
    return new SubscriptionNotFoundException("Suscripción no encontrada: " + subscriptionId);
  }

  public static SubscriptionNotFoundException noActiveForUser(UUID userId) {
    return new SubscriptionNotFoundException("El usuario " + userId + " no tiene una suscripción activa");
  }
}
