package pe.smartcash.cash.subscription.domain.exception;

import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public class ActiveSubscriptionAlreadyExistsException extends RuntimeException {

  public ActiveSubscriptionAlreadyExistsException(UserId userId) {
    super("El usuario " + userId.value() + " ya tiene una suscripción activa");
  }
}
