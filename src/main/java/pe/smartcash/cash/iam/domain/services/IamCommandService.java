package pe.smartcash.cash.iam.domain.services;

import pe.smartcash.cash.iam.domain.model.commands.LogoutCommand;
import pe.smartcash.cash.iam.domain.model.commands.RefreshTokenCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignInCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignUpCommand;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/** Contrato de escritura del bounded context: vive en domain, la implementación vive en application. */
public interface IamCommandService {

  UserId handle(SignUpCommand command);

  TokenPair handle(SignInCommand command);

  TokenPair handle(RefreshTokenCommand command);

  void handle(LogoutCommand command);
}
