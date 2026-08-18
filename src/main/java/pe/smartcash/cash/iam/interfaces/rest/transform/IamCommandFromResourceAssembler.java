package pe.smartcash.cash.iam.interfaces.rest.transform;

import pe.smartcash.cash.iam.domain.model.commands.SignInCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignUpCommand;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignInResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResource;

public final class IamCommandFromResourceAssembler {

  private IamCommandFromResourceAssembler() {}

  public static SignUpCommand toSignUpCommand(SignUpResource resource) {
    return new SignUpCommand(resource.email(), resource.password(), resource.displayName());
  }

  public static SignInCommand toSignInCommand(SignInResource resource) {
    return new SignInCommand(resource.email(), resource.password());
  }
}
