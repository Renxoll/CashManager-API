package pe.smartcash.cash.profile.domain.exception;

import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

public class UserProfileNotFoundException extends RuntimeException {

  public UserProfileNotFoundException(UserId userId) {
    super("No existe un perfil para el usuario: " + userId.value());
  }
}
