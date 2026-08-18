package pe.smartcash.cash.iam.domain.exception;

import pe.smartcash.cash.iam.domain.model.valueobjects.Email;

public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException(Email email) {
    super("Ya existe una cuenta registrada con el email: " + email.value());
  }
}
