package pe.smartcash.cash.transactions.domain.exception;

import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/** El userId referenciado por el webhook no existe (visto desde este bounded context). */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UserId userId) {
    super("Usuario no encontrado: " + userId.value());
  }
}
