package pe.smartcash.cash.profile.domain.exception;

import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

/** Ya existe un perfil para ese userId; register() no es un upsert. */
public class UserProfileAlreadyRegisteredException extends RuntimeException {

  public UserProfileAlreadyRegisteredException(UserId userId) {
    super("Ya existe un perfil registrado para el usuario: " + userId.value());
  }
}
